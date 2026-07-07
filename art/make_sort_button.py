"""Generates the 12x12 sort button texture (two states stacked: normal / hover)."""
from PIL import Image
import os

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   "..", "src", "main", "resources", "assets", "stackstorage", "textures", "gui", "sort_button.png")

# Legend: . transparent | # border | f face | t face top highlight | S letter
PIXELS = """
.##########.
#tttttttttt#
#tttttttttt#
#fffSSSffff#
#ffSfffSfff#
#ffSfffffff#
#fffSSSffff#
#ffffffSfff#
#ffSfffSfff#
#fffSSSffff#
#ffffffffff#
.##########.
"""

def plate(border, face, face_top, letter):
    colors = {
        '.': (0, 0, 0, 0),
        '#': border,
        'f': face,
        't': face_top,
        'S': letter,
    }
    rows = PIXELS.strip().splitlines()
    img = Image.new('RGBA', (12, 12))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            img.putpixel((x, y), colors[ch])
    return img

normal = plate((70, 70, 70, 255), (150, 150, 150, 255), (180, 180, 180, 255), (35, 35, 35, 255))
hover = plate((255, 255, 255, 255), (180, 180, 200, 255), (210, 210, 228, 255), (25, 25, 25, 255))

sheet = Image.new('RGBA', (12, 24))
sheet.paste(normal, (0, 0))
sheet.paste(hover, (0, 12))
sheet.save(os.path.abspath(OUT))

preview = os.path.join(os.path.dirname(os.path.abspath(__file__)), "sort_preview.png")
sheet.resize((120, 240), Image.NEAREST).save(preview)
print("done")
