"""Run actual Forge/ProjectE/RSI ledger tests on a temporary, loopback-only server.

Requires cached Gradle dependencies, JDK 17+, and an already accepted eula.txt.
Does not accept the Minecraft EULA on the user's behalf. No existing world is opened.
"""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

sys.dont_write_bytecode = True
from test_rsi_arcane import java_home

PROJECT = Path(__file__).resolve().parents[2]


def groovy(path):
    return str(path.resolve()).replace("\\", "/").replace("'", "\\'")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=PROJECT)
    parser.add_argument("--eula", type=Path, default=PROJECT / "run/eula.txt")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    root = args.root.resolve()
    if not args.eula.is_file() or "eula=true" not in args.eula.read_text(encoding="utf-8").lower():
        raise RuntimeError("Supply an already accepted eula.txt using --eula PATH")
    config = json.loads((root / "src/main/resources/projectexa.mixins.json").read_text(encoding="utf-8"))
    enabled = "RsiMaterialSourcesMixin" in config["mixins"]
    (root / "build").mkdir(exist_ok=True)
    output = args.output.resolve() if args.output else Path(tempfile.mkdtemp(prefix="rsi-native-", dir=root / "build"))
    output.mkdir(parents=True, exist_ok=True)
    if (output / "native-smoke-result.txt").exists():
        raise RuntimeError("Use a fresh --output directory; refusing to reuse a stale result")
    shutil.copyfile(args.eula, output / "eula.txt")
    (output / "server.properties").write_text(
        "server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nlevel-name=smoke\n"
        "level-type=minecraft:flat\ngenerate-structures=false\nspawn-protection=0\n"
        "view-distance=2\nsimulation-distance=2\n", encoding="utf-8")
    init = output / "native.init.gradle"
    fixture = Path(__file__).parent / "rsi_native"
    init.write_text("""allprojects {
    afterEvaluate { p ->
        p.sourceSets.main.java.srcDir p.file('%s')
        p.minecraft.runs.server {
            workingDirectory p.file('%s')
            property 'forge.logging.console.level', 'warn'
            property 'rsi.smoke.expect', '%s'
            args '--nogui'
        }
    }
}
""" % (groovy(fixture), groovy(output), str(enabled).lower()), encoding="utf-8")
    env = os.environ.copy()
    env["JAVA_HOME"] = str(java_home())
    env["PATH"] = str(Path(env["JAVA_HOME"]) / "bin") + os.pathsep + env["PATH"]
    wrapper = root / ("gradlew.bat" if os.name == "nt" else "gradlew")
    base = [str(wrapper)]
    if os.name == "nt":
        base = ["cmd.exe", "/c", str(wrapper)]
    command = base + ["-I", str(init), "runServer", "--offline", "--console=plain"]
    print("COMMAND:", subprocess.list2cmdline(command), flush=True)
    with (output / "stdout.txt").open("wb") as out, (output / "stderr.txt").open("wb") as err:
        run = subprocess.run(command, cwd=root, env=env, stdout=out, stderr=err, timeout=300)
    result_path = output / "native-smoke-result.txt"
    result = result_path.read_text(encoding="utf-8").strip() if result_path.exists() else "FAIL no result"
    print("GRADLE_EXIT:", run.returncode)
    print(result)
    print("EVIDENCE:", output)
    # Drop the test-only subscriber from normal build outputs before returning.
    with (output / "cleanup.stdout.txt").open("wb") as out, (output / "cleanup.stderr.txt").open("wb") as err:
        cleanup = subprocess.run(base + ["compileJava", "--offline", "--console=plain"],
                                 cwd=root, env=env, stdout=out, stderr=err, timeout=180)
    if run.returncode or not result.startswith("PASS ") or cleanup.returncode:
        sys.exit(1)


if __name__ == "__main__":
    main()
