#!/usr/bin/env bash
# Takes the screenshots the README and docs/ use.
#
#   ./Tools/gen-screenshots.sh docs/images
#
# Run by hand against a booted emulator, never in CI. A morph is a transition,
# so a still frame is a poor witness: these are for the README, and what
# actually holds the behaviour is the fixtures and MorphPlan.frame(elapsed).
#
# The activity takes a `screen` or a `demo` extra so a shot can be aimed without
# a chain of taps. That is the only reason those extras exist.
set -euo pipefail

out=${1:-docs/images}
package=io.github.dim971.textmorph.showcase
activity="$package/.ShowcaseActivity"
mkdir -p "$out"

shot() {
  local name=$1
  shift
  adb shell am force-stop "$package"
  adb shell am start -n "$activity" "$@" > /dev/null
  # Long enough for the first morph of a self-advancing preview to be part way
  # through, which is the only interesting moment to photograph.
  sleep 3
  adb exec-out screencap -p > "$out/$name.png"
  echo "wrote $out/$name.png"
}

crop() {
  # A phone screenshot is mostly empty below a demo, and a README thumbnail of
  # it is mostly empty too. One height for the whole set, so they stay
  # consistent. Skipped rather than fatal where Pillow is not installed: the
  # uncropped shots are still correct, just tall.
  if ! python3 -c "import PIL" 2> /dev/null; then
    echo "Pillow not installed; leaving the shots uncropped"
    return
  fi
  python3 - "$out" <<'PYEOF'
import glob
import os
import sys

from PIL import Image

for path in sorted(glob.glob(os.path.join(sys.argv[1], "*.png"))):
    image = Image.open(path)
    width, height = image.size
    image.crop((0, 0, width, int(height * 0.62))).save(path, optimize=True)
    print(f"cropped {path}")
PYEOF
}

shot catalog
shot playground --es screen Playground
shot about --es screen About
shot hero --es demo earned
shot wallet --es demo earned
shot ticker --es demo ticker
shot field --es demo amount
shot reflow --es demo resize

crop
