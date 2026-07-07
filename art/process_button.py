from PIL import Image, ImageDraw, ImageEnhance
import os

SRC = r"c:/Users/FacuS/OneDrive/Escritorio/Sora/Proyect/StackStorage/art/QuickStack.png"
OUT = r"c:/Users/FacuS/OneDrive/Escritorio/Sora/Proyect/StackStorage/src/main/resources/assets/stackstorage/textures/gui/quick_stack_button.png"
SCRATCH = os.path.dirname(os.path.abspath(__file__))

# Texture is 2x the logical button size (20x18) for crispness at GUI scale >= 2
W, H = 40, 36

img = Image.open(SRC).convert('RGBA')

# Key out the white background flood-filling from the corners (keeps white sparkles inside)
for corner in [(0, 0), (img.width - 1, 0), (0, img.height - 1), (img.width - 1, img.height - 1)]:
    ImageDraw.floodfill(img, corner, (0, 0, 0, 0), thresh=60)

# Crop to content
img = img.crop(img.getbbox())

# Pad to 10:9 (button aspect) centered so the resize doesn't distort
target_ratio = W / H
w, h = img.size
if w / h < target_ratio:
    new_w = int(h * target_ratio)
    canvas = Image.new('RGBA', (new_w, h), (0, 0, 0, 0))
    canvas.alpha_composite(img, ((new_w - w) // 2, 0))
    img = canvas
else:
    new_h = int(w / target_ratio)
    canvas = Image.new('RGBA', (w, new_h), (0, 0, 0, 0))
    canvas.alpha_composite(img, (0, (new_h - h) // 2))
    img = canvas

normal = img.resize((W, H), Image.LANCZOS)
nearest = img.resize((W, H), Image.NEAREST)

# Hover: brightened copy
hover = ImageEnhance.Brightness(normal).enhance(1.25)

sheet = Image.new('RGBA', (W, H * 2))
sheet.paste(normal, (0, 0))
sheet.paste(hover, (0, H))
sheet.save(OUT)

# Previews at 5x
normal.resize((W * 5, H * 5), Image.NEAREST).save(os.path.join(SCRATCH, "qs_lanczos.png"))
nearest.resize((W * 5, H * 5), Image.NEAREST).save(os.path.join(SCRATCH, "qs_nearest.png"))
sheet.resize((W * 5, H * 10), Image.NEAREST).save(os.path.join(SCRATCH, "button_preview.png"))
print("done", img.size)
