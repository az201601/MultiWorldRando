package com.multiworldrando.hmgbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.multiworldrando.core.SpoilerLog;

/**
 * Realigns the field's crop graphics with the shuffled crop seasons.
 * <p>
 * Crop stage tiles expand (via the metatile tables in bank $11) to shared BG
 * tile ids whose pixel art lives in the seasonal tile chunks - one pair of
 * chunks per season, in two parallel art sets (bank $0B loaded by the bank 1
 * map init, bank $2B loaded by the bank 21 CGB init). Vanilla only ever shows
 * each crop in its own season, so a shared id holds "the art of whichever crop
 * grows this season" (and is blank for Tomato/Corn/Broccoli stages outside
 * their vanilla season). After the season shuffle a crop planted in its new
 * season would render with another crop's art - or with nothing at all.
 * <p>
 * This fix rewrites, per seed and per season, the art at every crop-art id to
 * the art of the crop actually assigned to that season (taken from that crop's
 * vanilla chunk), for both art sets. When two same-family crops land in one
 * season (so one id would need two arts at once), the losing crop's metatile
 * rows are repointed to reuse ids whose slots are free in that season. Crops
 * only ever render in their assigned season (the field walks clear them
 * everywhere else at map load / sleep), so each crop's art is only needed in
 * its own season's chunks.
 * <p>
 * The recompressed chunks are relocated into free ROM space and every
 * seasonal load site is pointed at them. When the assignment is the vanilla
 * one every write is a no-op and the ROM is returned unchanged.
 */
final class CropArtFix {

    /** Seasonal chunk CPU addresses per art set: [set][season] = {hi->\$8800, lo->\$9000}. */
    private static final int[][][] SEASON_CHUNKS = {
        { { 0x4758, 0x4001 }, { 0x5457, 0x4C1A }, { 0x6150, 0x59FA }, { 0x6DC8, 0x6612 } },
        { { 0x478A, 0x4001 }, { 0x54C8, 0x4C4E }, { 0x6205, 0x5A75 }, { 0x6EC3, 0x66C9 } },
    };
    private static final int[] CHUNK_BANKS = { 0x0B, 0x2B };
    private static final int[] LOADER_BANK_FILES = { 0x04000, 0x84000 };

    /** Metatile tables in bank $11 and the byte-identical bank $31 copy. */
    private static final int[] METATILE_BANK_FILES = { 0x44000, 0xC4000 };
    private static final int TABLE_ATTR40 = 0x6BA9;   // crop cells carry attr & $C0 == $40
    private static final int TABLE_DEFAULT = 0x697D;
    private static final int TABLE_ATTR80 = 0x694D;

    /** Crop family tile blocks, index order of {@link SeedSeasonShufflePatch#CROP_NAMES}. */
    private static final int[][][] CROP_BLOCKS = {
        { { 0x90, 0x9F } },                   // Turnip
        { { 0xA0, 0xAF } },                   // Potato
        { { 0x50, 0x5F }, { 0x60, 0x6F } },   // Tomato
        { { 0x70, 0x7F }, { 0x80, 0x8F } },   // Corn
        { { 0xB0, 0xBF } },                   // Eggplant
        { { 0xC0, 0xCF } },                   // Peanut
        { { 0xD0, 0xDF } },                   // Carrot
        { { 0xE0, 0xEF }, { 0xF0, 0xFF } },   // Broccoli
    };
    private static final int[] GRASS_BLOCK = { 0x40, 0x4F };
    private static final int[] CROP_VANILLA_SEASON = { 0, 0, 1, 1, 2, 2, 3, 3 };

    private static final int NSETS = 2;
    private static final int NSEASONS = 4;
    private static final int TILE = 16;

    private CropArtFix() {
    }

    /**
     * Realigns crop stage graphics with the shuffled seasons.
     *
     * @param rom          the ROM to patch in place
     * @param seasonByCrop assigned season (0-3) per crop, index order of
     *                     {@link SeedSeasonShufflePatch#CROP_NAMES}
     * @param spoiler      spoiler log for the change summary
     */
    static void apply(byte[] rom, int[] seasonByCrop, SpoilerLog spoiler) throws Exception {
        // ---- decompress all 16 chunks; remember their original compressed sizes ----
        byte[][][] chunks = new byte[NSETS][NSEASONS * 2][];
        int[][] compLen = new int[NSETS][NSEASONS * 2];
        for (int s = 0; s < NSETS; s++) {
            for (int e = 0; e < NSEASONS; e++) {
                for (int k = 0; k < 2; k++) {
                    int cpu = SEASON_CHUNKS[s][e][k];
                    int off = CHUNK_BANKS[s] * 0x4000 + (cpu - 0x4000);
                    Decompressed d = decompressFull(rom, off, k == 0 ? 0x8800 : 0x9000);
                    chunks[s][e * 2 + k] = d.data;
                    compLen[s][e * 2 + k] = d.consumed;
                }
            }
        }

        // ---- crop tile types + every id's referrers (all 3 tables) ----
        boolean[] cropType = new boolean[256];
        for (int[][] blocks : CROP_BLOCKS) {
            for (int[] blk : blocks) {
                for (int t = blk[0]; t <= blk[1]; t++) {
                    cropType[t] = true;
                }
            }
        }
        for (int t = GRASS_BLOCK[0]; t <= GRASS_BLOCK[1]; t++) {
            cropType[t] = true;
        }
        int[][][] refTable = new int[256][16][2];
        int[] refCount = new int[256];
        int[] tableCpus = { TABLE_DEFAULT, TABLE_ATTR40, TABLE_ATTR80 };
        for (int tc = 0; tc < 3; tc++) {
            for (int t = 0; t < 256; t++) {
                int off = METATILE_BANK_FILES[0] + (tableCpus[tc] - 0x4000) + t * 4;
                for (int j = 0; j < 4; j++) {
                    int id = rom[off + j] & 0xFF;
                    int n = refCount[id];
                    if (n < 16) {
                        refTable[id][n][0] = tc;
                        refTable[id][n][1] = t;
                    }
                    refCount[id] = n + 1;
                }
            }
        }
        // crop-art id: every referrer is a crop tile row, or a dead alias row
        // (T80 rows of map tiles $F1-$F7 / T00 row of $DF - never seen in any map).
        boolean[] cropArt = new boolean[256];
        for (int i = 0; i < 256; i++) {
            cropArt[i] = true;
            for (int j = 0; j < refCount[i] && j < 16; j++) {
                int tc = refTable[i][j][0];
                int t = refTable[i][j][1];
                if (cropType[t]) {
                    continue;
                }
                if (tc == 2 && t >= 0xF1 && t <= 0xF7) {
                    continue;
                }
                if (tc == 0 && t == 0xDF) {
                    continue;
                }
                cropArt[i] = false;
                break;
            }
        }

        // ---- crop rows (T40 table offsets), users per id ----
        List<int[]> rowOffs = new ArrayList<>();       // per crop: table offsets
        Map<Integer, List<Integer>> idUsers = new HashMap<>();
        boolean[] usedEver = new boolean[256];
        for (int c = 0; c < 8; c++) {
            for (int[] blk : CROP_BLOCKS[c]) {
                for (int t = blk[0]; t <= blk[1]; t++) {
                    rowOffs.add(new int[] { c, METATILE_BANK_FILES[0] + (TABLE_ATTR40 - 0x4000) + t * 4 });
                    for (int j = 0; j < 4; j++) {
                        int id = rom[METATILE_BANK_FILES[0] + (TABLE_ATTR40 - 0x4000) + t * 4 + j] & 0xFF;
                        if (cropArt[id]) {
                            List<Integer> l = idUsers.get(id);
                            if (l == null) {
                                l = new ArrayList<>();
                                idUsers.put(id, l);
                            }
                            if (!l.contains(c)) {
                                l.add(c);
                            }
                            usedEver[id] = true;
                        }
                    }
                }
            }
        }

        // ---- reuse-id candidates: crop-art ids first (proven render path), lo half first ----
        List<Integer> candidates = new ArrayList<>();
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 256; i++) {
                if (cropArt[i] && usedEver[i] && ((i < 0x80) == (pass == 0))) {
                    candidates.add(i);
                }
            }
        }
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 256; i++) {
                if (refCount[i] == 0 && ((i < 0x80) == (pass == 0))) {
                    candidates.add(i);
                }
            }
        }

        // ---- plan writes per (set, season, id) and repoints per (crop, id) ----
        Map<Long, byte[]> writes = new HashMap<>();    // key = (set<<16)|(season<<8)|id
        Map<Long, Integer> repoint = new HashMap<>();  // key = (crop<<8)|id -> new id
        boolean changed = false;

        for (int e = 0; e < NSEASONS; e++) {
            boolean[] used = new boolean[256];
            for (int c = 0; c < 8; c++) {
                if (seasonByCrop[c] == e) {
                    for (int[] r : rowOffs) {
                        if (r[0] == c) {
                            for (int j = 0; j < 4; j++) {
                                used[rom[r[1] + j] & 0xFF] = true;
                            }
                        }
                    }
                }
            }
            for (int t = GRASS_BLOCK[0]; t <= GRASS_BLOCK[1]; t++) {
                int off = METATILE_BANK_FILES[0] + (TABLE_ATTR40 - 0x4000) + t * 4;
                for (int j = 0; j < 4; j++) {
                    used[rom[off + j] & 0xFF] = true;
                }
            }
            List<Integer> ids = new ArrayList<>(idUsers.keySet());
            ids.sort(null);
            for (int id : ids) {
                // group this season's users of the id by their vanilla art (both sets)
                List<List<Integer>> groups = new ArrayList<>();
                List<byte[]> arts = new ArrayList<>();
                for (int c : idUsers.get(id)) {
                    if (seasonByCrop[c] != e) {
                        continue;
                    }
                    byte[] key = new byte[NSETS * TILE];
                    for (int s = 0; s < NSETS; s++) {
                        byte[] van = chunks[s][CROP_VANILLA_SEASON[c] * 2 + (id < 0x80 ? 1 : 0)];
                        System.arraycopy(van, (id < 0x80 ? id : id - 0x80) * TILE, key, s * TILE, TILE);
                    }
                    int g = -1;
                    for (int q = 0; q < arts.size(); q++) {
                        if (Arrays.equals(arts.get(q), key)) {
                            g = q;
                            break;
                        }
                    }
                    if (g < 0) {
                        g = arts.size();
                        arts.add(key);
                        groups.add(new ArrayList<>());
                    }
                    groups.get(g).add(c);
                }
                if (groups.isEmpty()) {
                    continue;
                }
                for (int g = 0; g < groups.size(); g++) {
                    int target = id;
                    if (g > 0) {
                        target = -1;
                        for (int cand : candidates) {
                            if (!used[cand]) {
                                target = cand;
                                used[cand] = true;
                                break;
                            }
                        }
                        if (target < 0) {
                            throw new IllegalStateException(
                                    "CropArtFix: no free tile id for a same-season crop collision");
                        }
                        for (int c : groups.get(g)) {
                            repoint.put((((long) c) << 8) | id, target);
                        }
                        changed = true;
                    }
                    for (int s = 0; s < NSETS; s++) {
                        byte[] art = Arrays.copyOfRange(arts.get(g), s * TILE, s * TILE + TILE);
                        long key = (((long) s) << 16) | (e << 8) | (target & 0xFF);
                        byte[] cur = chunks[s][e * 2 + (target < 0x80 ? 1 : 0)];
                        int offIn = (target < 0x80 ? target : target - 0x80) * TILE;
                        if (!Arrays.equals(Arrays.copyOfRange(cur, offIn, offIn + TILE), art)) {
                            changed = true;
                        }
                        writes.put(key, art);
                    }
                }
            }
        }

        if (!changed) {
            return;
        }

        // ---- patch metatile rows (both banks) ----
        for (int[] r : rowOffs) {
            int c = r[0];
            for (int bank : METATILE_BANK_FILES) {
                int o = bank + (r[1] - METATILE_BANK_FILES[0]);
                for (int j = 0; j < 4; j++) {
                    Integer rep = repoint.get((((long) c) << 8) | (rom[r[1] + j] & 0xFF));
                    if (rep != null) {
                        rom[o + j] = (byte) (int) rep;
                    }
                }
            }
        }

        // ---- apply art writes to the chunk buffers ----
        for (Map.Entry<Long, byte[]> w : writes.entrySet()) {
            long key = w.getKey();
            int s = (int) (key >> 16);
            int e = (int) ((key >> 8) & 0xFF);
            int id = (int) (key & 0xFF);
            byte[] buf = chunks[s][e * 2 + (id < 0x80 ? 1 : 0)];
            System.arraycopy(w.getValue(), 0, buf, (id < 0x80 ? id : id - 0x80) * TILE, TILE);
        }

        // ---- compress all 16 chunks ----
        byte[][] comp = new byte[NSETS * NSEASONS * 2][];
        for (int s = 0; s < NSETS; s++) {
            for (int i = 0; i < NSEASONS * 2; i++) {
                comp[s * NSEASONS * 2 + i] = compress(chunks[s][i]);
            }
        }

        // ---- free space: the original chunk spans + 00/FF runs (>= 128 bytes) ----
        List<int[]> runs = new ArrayList<>();   // {start, length}
        for (int s = 0; s < NSETS; s++) {
            for (int e = 0; e < NSEASONS; e++) {
                for (int k = 0; k < 2; k++) {
                    int cpu = SEASON_CHUNKS[s][e][k];
                    int off = CHUNK_BANKS[s] * 0x4000 + (cpu - 0x4000);
                    runs.add(new int[] { off, compLen[s][e * 2 + k] });
                }
            }
        }
        int i = 0;
        while (i < rom.length) {
            int v = rom[i] & 0xFF;
            if (v == 0x00 || v == 0xFF) {
                int j = i;
                while (j < rom.length && (rom[j] & 0xFF) == v) {
                    j++;
                }
                if (j - i >= 128) {
                    runs.add(new int[] { i, j - i });
                }
                i = j;
            } else {
                i++;
            }
        }
        // largest first, best fit per chunk
        runs.sort((a, b) -> b[1] - a[1]);
        int[] compOrder = new int[comp.length];
        for (int q = 0; q < comp.length; q++) {
            compOrder[q] = q;
        }
        Integer[] order = Arrays.stream(compOrder).boxed().toArray(Integer[]::new);
        Arrays.sort(order, (a, b) -> comp[b].length - comp[a].length);
        int[] placeAt = new int[comp.length];
        Arrays.fill(placeAt, -1);
        for (int q : order) {
            int need = comp[q].length + 1;
            int best = -1;
            for (int r = 0; r < runs.size(); r++) {
                if (runs.get(r)[1] >= need && (best < 0 || runs.get(r)[1] < runs.get(best)[1])) {
                    best = r;
                }
            }
            if (best < 0) {
                throw new IllegalStateException("CropArtFix: no free ROM space for recompressed tiles");
            }
            int[] run = runs.get(best);
            placeAt[q] = run[0];
            run[0] += need;
            run[1] -= need;
        }
        for (int q = 0; q < comp.length; q++) {
            System.arraycopy(comp[q], 0, rom, placeAt[q], comp[q].length);
        }

        // ---- collect loader sites from the ORIGINAL bytes, then patch ----
        List<int[]> patchList = new ArrayList<>();   // {site, addr}
        for (int s = 0; s < NSETS; s++) {
            for (int e = 0; e < NSEASONS; e++) {
                for (int k = 0; k < 2; k++) {
                    int cpu = SEASON_CHUNKS[s][e][k];
                    int lb = LOADER_BANK_FILES[s];
                    int q = findSeq(rom, lb, lb + 0x4000, new int[] {
                            0x21, cpu & 0xFF, (cpu >> 8) & 0xFF, 0x0E, CHUNK_BANKS[s],
                            0x11, 0x00, k == 0 ? 0x88 : 0x90 });
                    if (q < 0) {
                        throw new IllegalStateException(String.format(
                                "CropArtFix: loader site missing (set %d season %d half %d)", s, e, k));
                    }
                    patchList.add(new int[] { q, placeAt[s * NSEASONS * 2 + e * 2 + k] });
                }
            }
        }
        for (int[] p : patchList) {
            int addr = p[1];
            rom[p[0]] = 0x21;
            rom[p[0] + 1] = (byte) (((addr % 0x4000) + 0x4000) & 0xFF);
            rom[p[0] + 2] = (byte) ((((addr % 0x4000) + 0x4000) >> 8) & 0xFF);
            rom[p[0] + 3] = 0x0E;
            rom[p[0] + 4] = (byte) (addr / 0x4000);
        }

        spoiler.addChange("Crop stage graphics",
                "re-aligned to the shuffled seasons (both CGB/DMG tile sets)");
    }

    /** Finds the first occurrence of the byte pattern in rom[from, to). */
    private static int findSeq(byte[] rom, int from, int to, int[] seq) {
        for (int i = from; i <= to - seq.length; i++) {
            boolean ok = true;
            for (int j = 0; j < seq.length; j++) {
                if ((rom[i + j] & 0xFF) != seq[j]) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return i;
            }
        }
        return -1;
    }

    private static final class Decompressed {
        byte[] data;
        int consumed;
    }

    /** Faithful port of the game's LZ decompressor (Call_000_31a0). */
    private static Decompressed decompressFull(byte[] rom, int off, int dest) {
        int size = (rom[off] & 0xFF) | ((rom[off + 1] & 0xFF) << 8);
        byte[] out = new byte[size];
        int p = off + 2;
        int made = 0;
        int ctrl = 0;
        int bits = 0;
        while (made < size) {
            if (bits == 0) {
                ctrl = rom[p++] & 0xFF;
                bits = 8;
            }
            int flag = ctrl & 1;
            ctrl >>= 1;
            bits--;
            if (flag == 1) {
                out[made++] = rom[p++];
            } else {
                int b0 = rom[p++] & 0xFF;
                int b1 = rom[p++] & 0xFF;
                int length = (b0 & 0x0F) + 3;
                int offset = ((b1 << 8) | b0) >> 4;
                int src = dest + made - offset;
                if (src >= dest) {
                    int idx = src - dest;
                    for (int n = 0; n < length; n++) {
                        out[made++] = out[idx++];
                    }
                } else {
                    int srcl = src & 0xFF;
                    int z = srcl == 0 ? length : ((-srcl) & 0xFF);
                    for (int n = 0; n < length; n++) {
                        out[made++] = (byte) (n < z ? 0 : out[n - z]);
                    }
                }
            }
        }
        Decompressed d = new Decompressed();
        d.data = out;
        d.consumed = p - off;
        return d;
    }

    /** Compressor for the game's LZ format (greedy with lazy matching). */
    private static byte[] compress(byte[] data) {
        ArrayList<Byte> out = new ArrayList<>();
        out.add((byte) (data.length & 0xFF));
        out.add((byte) ((data.length >> 8) & 0xFF));
        int i = 0;
        int ctrlPos = -1;
        int flags = 0;
        int nflags = 0;
        while (i < data.length) {
            int[] m = findMatch(data, i);
            int bestLen = m[0];
            int bestOff = m[1];
            if (bestLen >= 3 && bestLen < 18 && i + 1 < data.length) {
                int[] m2 = findMatch(data, i + 1);
                if (m2[0] > bestLen) {
                    bestLen = 0;
                    bestOff = 0;
                }
            }
            if (bestLen >= 3) {
                if (ctrlPos < 0) {
                    ctrlPos = out.size();
                    out.add((byte) 0);
                    flags = 0;
                    nflags = 0;
                }
                flags |= (0 << nflags);
                nflags++;
                if (nflags == 8) {
                    out.set(ctrlPos, (byte) flags);
                    ctrlPos = -1;
                }
                out.add((byte) (((bestOff & 0xF) << 4) | (bestLen - 3)));
                out.add((byte) ((bestOff >> 4) & 0xFF));
                i += bestLen;
            } else {
                if (ctrlPos < 0) {
                    ctrlPos = out.size();
                    out.add((byte) 0);
                    flags = 0;
                    nflags = 0;
                }
                flags |= (1 << nflags);
                nflags++;
                if (nflags == 8) {
                    out.set(ctrlPos, (byte) flags);
                    ctrlPos = -1;
                }
                out.add(data[i]);
                i++;
            }
        }
        if (ctrlPos >= 0) {
            out.set(ctrlPos, (byte) flags);
        }
        byte[] res = new byte[out.size()];
        for (int q = 0; q < out.size(); q++) {
            res[q] = out.get(q);
        }
        return res;
    }

    private static int[] findMatch(byte[] data, int pos) {
        int bestLen = 0;
        int bestOff = 0;
        int lo = Math.max(0, pos - 4095);
        for (int j = pos - 1; j >= lo; j--) {
            if (data[j] == data[pos]) {
                int l = 0;
                while (l < 18 && pos + l < data.length && data[j + l] == data[pos + l]) {
                    l++;
                }
                if (l > bestLen) {
                    bestLen = l;
                    bestOff = pos - j;
                    if (l == 18) {
                        break;
                    }
                }
            }
        }
        return new int[] { bestLen, bestOff };
    }
}
