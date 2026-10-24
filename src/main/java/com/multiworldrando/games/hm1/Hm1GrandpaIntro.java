package com.multiworldrando.games.hm1;

import java.util.ArrayList;
import java.util.List;

public class Hm1GrandpaIntro {

    public static final int TOTAL_ALLOCATED_BYTES = 19 * 43; // 817 bytes
    public static final int ROM_OFFSET_DMG = 0x029110;       // Bank 0x0A: $5110
    public static final int ROM_OFFSET_GBC = 0x0A9110;       // Bank 0x2A: $5110

    public static class DialoguePage {
        public final String line1;
        public final String line2;

        public DialoguePage(String line1, String line2) {
            this.line1 = line1;
            this.line2 = line2;
        }
    }

    public static List<DialoguePage> getDefaultScript() {
        List<DialoguePage> script = new ArrayList<>();
        script.add(new DialoguePage("<PLAYER>! Wake up,", "it is Grandpa!"));
        script.add(new DialoguePage("Wait, something", "is very wrong!"));
        script.add(new DialoguePage("This is not your", "usual farm..."));
        script.add(new DialoguePage("The multiverse", "has fractured!"));
        script.add(new DialoguePage("Your tools and", "precious items"));
        script.add(new DialoguePage("are scattered in", "other worlds!"));
        script.add(new DialoguePage("Search every-", "where you can!"));
        script.add(new DialoguePage("You might even", "find items for"));
        script.add(new DialoguePage("another game", "along the way!"));
        script.add(new DialoguePage("Can you still", "surpass me as"));
        script.add(new DialoguePage("Ranch Master", "in this chaos??"));
        script.add(new DialoguePage("At the end of", "winter, I will"));
        script.add(new DialoguePage("come and check", "your progress!"));
        script.add(new DialoguePage("One warning from", "the grave..."));
        script.add(new DialoguePage("Crops STILL die", "at season end!"));
        script.add(new DialoguePage("Good luck in", "the MultiWorld!"));
        script.add(new DialoguePage("Now get to work,", "<PLAYER>!"));
        return script;
    }

    public static byte[] buildCutsceneBlob(List<DialoguePage> script) {
        if (script == null) {
            script = getDefaultScript();
        }

        byte[] blob = new byte[TOTAL_ALLOCATED_BYTES];
        int writePos = 0;

        for (int i = 0; i < script.size(); i++) {
            boolean isLast = (i == script.size() - 1);
            byte[] page = Hm1TextEncoder.makePage(script.get(i).line1, script.get(i).line2, isLast);
            System.arraycopy(page, 0, blob, writePos, page.length);
            writePos += page.length;
        }

        // Remaining bytes in blob are already 0x00
        return blob;
    }

    public static void patchGrandpaIntro(byte[] rom, List<DialoguePage> script) {
        byte[] blob = buildCutsceneBlob(script);
        System.arraycopy(blob, 0, rom, ROM_OFFSET_DMG, TOTAL_ALLOCATED_BYTES);
        System.arraycopy(blob, 0, rom, ROM_OFFSET_GBC, TOTAL_ALLOCATED_BYTES);
    }
}
