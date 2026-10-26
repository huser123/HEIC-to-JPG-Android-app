"""Regenerate synthetic test assets. Development only: pip install pillow pillow-heif."""
from pathlib import Path

from PIL import Image, ImageDraw
import pillow_heif

pillow_heif.register_heif_opener()
destination = Path(__file__).resolve().parents[1] / "app/src/androidTest/assets"
destination.mkdir(parents=True, exist_ok=True)
image = Image.new("RGB", (96, 64), "white")
draw = ImageDraw.Draw(image)
draw.rectangle((0, 0, 47, 63), fill="red")
draw.rectangle((48, 0, 95, 63), fill="blue")
image.save(destination / "normal.heic", quality=95)
exif = Image.Exif()
exif[274] = 6
image.save(destination / "rotated.heic", quality=95, exif=exif.tobytes())
image.save(destination / "not-heic.png")
(destination / "broken.heic").write_bytes(b"this is not an image")
