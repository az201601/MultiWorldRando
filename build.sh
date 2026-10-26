#!/bin/bash
# Build MultiWorldRando.jar (Java 8 compatible bytecode).
#
# Compiling uses the ECJ batch compiler (org.eclipse.jdt.core.compiler.batch),
# run on a plain JRE. Point ECJ_JAR at your copy, or drop it at the default path.
set -e
cd "$(dirname "$0")"

ECJ_JAR="${ECJ_JAR:-/home/user/tools/ecj/ecj.jar}"
if [ ! -f "$ECJ_JAR" ]; then
    echo "error: ECJ compiler jar not found at $ECJ_JAR (set ECJ_JAR env var)" >&2
    exit 1
fi

rm -rf out
mkdir -p out

echo "Compiling (target: Java 8)..."
SOURCES=$(find src -name '*.java' | sort)
java -cp "$ECJ_JAR" org.eclipse.jdt.internal.compiler.batch.Main \
    -source 1.8 -target 1.8 -nowarn -encoding UTF-8 -d out $SOURCES

echo "Packaging jar..."
mkdir -p out/META-INF
printf 'Manifest-Version: 1.0\nMain-Class: com.multiworldrando.MultiWorldRando\n' > out/META-INF/MANIFEST.MF
python3 - <<'PY'
import os, zipfile
with zipfile.ZipFile('MultiWorldRando.jar', 'w', zipfile.ZIP_DEFLATED) as z:
    z.write('out/META-INF/MANIFEST.MF', 'META-INF/MANIFEST.MF')
    for root, _, files in os.walk('out'):
        for f in sorted(files):
            p = os.path.join(root, f)
            arc = os.path.relpath(p, 'out')
            if arc == 'META-INF/MANIFEST.MF':
                continue
            z.write(p, arc)
PY

echo "Built MultiWorldRando.jar"
