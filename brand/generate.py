#!/usr/bin/env python3
"""Generate every PrivacyGuard brand asset from one set of shapes.

The logo is drawn on the 108x108 adaptive-icon canvas. Everything that
matters sits inside the 66-unit safe circle, so launchers can mask it to
any shape. Running this script rewrites the SVGs and PNGs in this folder
and the launcher VectorDrawables in app/src/main/res/drawable.

Needs librsvg and pycairo through PyGObject, plus Pillow:
    python3 brand/generate.py
"""
import io
import os

import cairo
import gi
from PIL import Image

gi.require_version("Rsvg", "2.0")
from gi.repository import Rsvg  # noqa: E402

BRAND = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(BRAND, "..", "app", "src", "main", "res")

# Palette
BG_CENTER = "#13604F"
BG_EDGE = "#062621"
FACET_TL = "#2ED8A8"
FACET_TR = "#5CF0C8"
FACET_BL = "#14B98A"
FACET_BR = "#0C9A70"
KEYHOLE = "#0A2F29"
ORBIT = "#4DF0C6"
INK = "#0A2F29"
WHITE = "#FFFFFF"

# Shapes, in 108x108 adaptive-icon units
SHIELD = ("M54,32 C60,35 66,36 71,36 V52 C71,63 63,71 54,75 "
          "C45,71 37,63 37,52 V36 C42,36 48,35 54,32 Z")
KEYHOLE_PATH = "M51.7,54.66 A5.2,5.2 0 1,1 56.3,54.66 L57.4,64 H50.6 Z"
FACET_SPLIT_Y = 53
ORBIT_CX, ORBIT_CY, ORBIT_RX, ORBIT_RY, ORBIT_ROT = 54, 57, 30, 8.8, -14
ORBIT_BACK = (f"M{ORBIT_CX - ORBIT_RX},{ORBIT_CY} "
              f"A{ORBIT_RX},{ORBIT_RY} 0 0,1 {ORBIT_CX + ORBIT_RX},{ORBIT_CY}")
ORBIT_FRONT = (f"M{ORBIT_CX - ORBIT_RX},{ORBIT_CY} "
               f"A{ORBIT_RX},{ORBIT_RY} 0 0,0 {ORBIT_CX + ORBIT_RX},{ORBIT_CY}")
ORBIT_WIDTH = 2.4
ORBIT_GAP_WIDTH = 5.0
FACETS = [
    (FACET_TL, f"M0,0 H54 V{FACET_SPLIT_Y} H0 Z"),
    (FACET_TR, f"M54,0 H108 V{FACET_SPLIT_Y} H54 Z"),
    (FACET_BL, f"M0,{FACET_SPLIT_Y} H54 V108 H0 Z"),
    (FACET_BR, f"M54,{FACET_SPLIT_Y} H108 V108 H54 Z"),
]

# --------------------------------------------------------------------- SVG

ROT = f"rotate({ORBIT_ROT} {ORBIT_CX} {ORBIT_CY})"


def svg_defs():
    return f"""<defs>
    <radialGradient id="bg" gradientUnits="userSpaceOnUse" cx="54" cy="48" r="62">
      <stop offset="0" stop-color="{BG_CENTER}"/>
      <stop offset="1" stop-color="{BG_EDGE}"/>
    </radialGradient>
    <clipPath id="shield"><path d="{SHIELD}"/></clipPath>
  </defs>"""


def svg_background():
    return '<rect width="108" height="108" fill="url(#bg)"/>'


def svg_foreground():
    facets = "".join(f'<path d="{d}" fill="{c}"/>' for c, d in FACETS)
    return f"""<path d="{ORBIT_BACK}" transform="{ROT}" fill="none" stroke="{ORBIT}" stroke-width="{ORBIT_WIDTH}" stroke-linecap="round"/>
  <g clip-path="url(#shield)">{facets}
    <path d="{ORBIT_FRONT}" transform="{ROT}" fill="none" stroke="{KEYHOLE}" stroke-width="{ORBIT_GAP_WIDTH}"/>
  </g>
  <path d="{KEYHOLE_PATH}" fill="{KEYHOLE}"/>
  <path d="{ORBIT_FRONT}" transform="{ROT}" fill="none" stroke="{ORBIT}" stroke-width="{ORBIT_WIDTH}" stroke-linecap="round"/>"""


def svg_monochrome(color):
    return f"""<path d="{SHIELD} {KEYHOLE_PATH}" fill="{color}" fill-rule="evenodd"/>
  <path d="{ORBIT_BACK} {ORBIT_FRONT}" transform="{ROT}" fill="none" stroke="{color}" stroke-width="{ORBIT_WIDTH}" stroke-linecap="round"/>"""


def svg_doc(body, viewbox="0 0 108 108", size=None):
    w, h = (size or (viewbox.split()[2], viewbox.split()[3]))
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{viewbox}" '
            f'width="{w}" height="{h}">\n  {svg_defs()}\n  {body}\n</svg>\n')


# The launcher shows the middle 72 units of the 108 canvas; full icons crop to it.
VISIBLE = "18 18 72 72"


def icon_svg(shape="square"):
    """Full icon: background + mark, cropped to the visible 72-unit area."""
    body = f"{svg_background()}\n  {svg_foreground()}"
    if shape == "circle":
        body = f'<clipPath id="c"><circle cx="54" cy="54" r="36"/></clipPath><g clip-path="url(#c)">{body}</g>'
    elif shape == "rounded":
        # Legacy launcher square, as Android Studio's Image Asset tool draws it.
        body = f'<clipPath id="c"><rect x="21" y="21" width="66" height="66" rx="10"/></clipPath><g clip-path="url(#c)">{body}</g>'
    return svg_doc(body, VISIBLE)


def mark_svg():
    """Mark only, transparent, trimmed to the shield and orbit."""
    return svg_doc(svg_foreground(), "22 30 64 48")


def mono_svg(color):
    return svg_doc(svg_monochrome(color), "22 30 64 48")


def lockup_svg(dark):
    """Horizontal lockup: icon + wordmark, for README, website and slides."""
    text = WHITE if dark else INK
    sub = ORBIT if dark else FACET_BR
    bg = f'<rect x="-40" y="-40" width="1200" height="400" fill="{BG_EDGE}"/>' if dark else ""
    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1120 320" width="1120" height="320">
  {svg_defs()}
  {bg}
  <clipPath id="r"><rect x="18" y="18" width="72" height="72" rx="16"/></clipPath>
  <g transform="translate(-20,-20) scale(3.3333)" clip-path="url(#r)">{svg_background()}{svg_foreground()}</g>
  <text x="300" y="178" font-family="Adwaita Sans, Inter, Roboto, Helvetica, Arial, sans-serif" font-weight="700" font-size="112" fill="{text}">PrivacyGuard</text>
  <text x="304" y="236" font-family="Adwaita Sans, Inter, Roboto, Helvetica, Arial, sans-serif" font-weight="500" font-size="40" fill="{sub}">Ad and tracker blocker for Android</text>
</svg>
"""


def feature_graphic_svg():
    font = "Adwaita Sans, Inter, Roboto, Helvetica, Arial, sans-serif"
    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 500" width="1024" height="500">
  <defs>
    <radialGradient id="fg" gradientUnits="userSpaceOnUse" cx="280" cy="250" r="760">
      <stop offset="0" stop-color="{BG_CENTER}"/>
      <stop offset="1" stop-color="{BG_EDGE}"/>
    </radialGradient>
    <clipPath id="shield"><path d="{SHIELD}"/></clipPath>
  </defs>
  <rect width="1024" height="500" fill="url(#fg)"/>
  <g transform="translate(-75,-85) scale(6.2)">{svg_foreground()}</g>
  <text x="500" y="232" font-family="{font}" font-weight="700" font-size="72" fill="{WHITE}">PrivacyGuard</text>
  <text x="502" y="290" font-family="{font}" font-weight="500" font-size="32" fill="{ORBIT}">Block trackers and scams</text>
  <text x="502" y="330" font-family="{font}" font-weight="500" font-size="32" fill="{ORBIT}">on your whole phone</text>
  <text x="502" y="388" font-family="{font}" font-weight="400" font-size="22" fill="#9FD9C8">No root · No account · Nothing leaves your phone</text>
</svg>
"""


# ------------------------------------------------------------------ render

def render(svg, width, height=None):
    height = height or width
    handle = Rsvg.Handle.new_from_data(svg.encode())
    surface = cairo.ImageSurface(cairo.FORMAT_ARGB32, width, height)
    ctx = cairo.Context(surface)
    rect = Rsvg.Rectangle()
    rect.x, rect.y, rect.width, rect.height = 0, 0, width, height
    handle.render_document(ctx, rect)
    buf = io.BytesIO()
    surface.write_to_png(buf)
    buf.seek(0)
    return Image.open(buf).convert("RGBA")


def write(rel, data):
    path = os.path.join(BRAND, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(data)


def save(img, rel, opaque=False):
    path = os.path.join(BRAND, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if opaque:
        img = img.convert("RGB")
    img.save(path, optimize=True)


def mark_png(width):
    # The mark viewBox is 64x48.
    return render(mark_svg(), width, round(width * 48 / 64))


# --------------------------------------------------------- VectorDrawables

VD_HEAD = """<?xml version="1.0" encoding="utf-8"?>
<!-- {comment} Generated by brand/generate.py; edit that script, not this file. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"{extra}
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
"""


def vd_orbit(path_data, color, width, cap="round"):
    return f"""    <group
        android:rotation="{ORBIT_ROT}"
        android:pivotX="{ORBIT_CX}"
        android:pivotY="{ORBIT_CY}">
        <path
            android:strokeColor="{color}"
            android:strokeWidth="{width}"
            android:strokeLineCap="{cap}"
            android:pathData="{path_data}" />
    </group>
"""


def vd_background():
    return VD_HEAD.format(
        comment="Launcher icon background: dark teal radial gradient.",
        extra='\n    xmlns:aapt="http://schemas.android.com/aapt"') + f"""    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="radial"
                android:centerX="54"
                android:centerY="48"
                android:gradientRadius="62">
                <item android:offset="0" android:color="{BG_CENTER}" />
                <item android:offset="1" android:color="{BG_EDGE}" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
"""


def vd_foreground():
    facets = "".join(f"""        <path
            android:fillColor="{c}"
            android:pathData="{d}" />
""" for c, d in FACETS)
    gap = vd_orbit(ORBIT_FRONT, KEYHOLE, ORBIT_GAP_WIDTH, "butt").replace("\n    ", "\n        ")
    return (VD_HEAD.format(
        comment="Launcher icon: faceted shield with a keyhole and an orbit ring, inside the 66dp safe zone.",
        extra="")
        + vd_orbit(ORBIT_BACK, ORBIT, ORBIT_WIDTH)
        + f"""    <group>
        <clip-path android:pathData="{SHIELD}" />
{facets}    {gap}    </group>
    <path
        android:fillColor="{KEYHOLE}"
        android:pathData="{KEYHOLE_PATH}" />
"""
        + vd_orbit(ORBIT_FRONT, ORBIT, ORBIT_WIDTH)
        + "</vector>\n")


def vd_monochrome():
    return (VD_HEAD.format(
        comment="Themed (monochrome) launcher icon: only the alpha is used.",
        extra="")
        + f"""    <path
        android:fillColor="#FFFFFFFF"
        android:fillType="evenOdd"
        android:pathData="{SHIELD} {KEYHOLE_PATH}" />
"""
        + vd_orbit(f"{ORBIT_BACK} {ORBIT_FRONT}", "#FFFFFFFF", ORBIT_WIDTH)
        + "</vector>\n")


# -------------------------------------------------------------------- main

def main():
    # Vector sources
    write("svg/icon.svg", icon_svg())
    write("svg/icon-round.svg", icon_svg("circle"))
    write("svg/mark.svg", mark_svg())
    write("svg/mark-white.svg", mono_svg(WHITE))
    write("svg/mark-black.svg", mono_svg("#000000"))
    write("svg/adaptive-background.svg", svg_doc(svg_background()))
    write("svg/adaptive-foreground.svg", svg_doc(svg_foreground()))
    write("svg/adaptive-monochrome.svg", svg_doc(svg_monochrome(WHITE)))
    write("svg/lockup-dark.svg", lockup_svg(dark=True))
    write("svg/lockup-light.svg", lockup_svg(dark=False))
    write("svg/feature-graphic.svg", feature_graphic_svg())

    # Google Play listing
    save(render(icon_svg(), 512), "play-store/icon-512.png")
    save(render(feature_graphic_svg(), 1024, 500), "play-store/feature-graphic-1024x500.png", opaque=True)

    # Android launcher: adaptive layers at xxxhdpi (108dp x 4) and legacy mipmaps
    save(render(svg_doc(svg_background()), 432), "android/adaptive/ic_launcher_background.png")
    save(render(svg_doc(svg_foreground()), 432), "android/adaptive/ic_launcher_foreground.png")
    save(render(svg_doc(svg_monochrome(WHITE)), 432), "android/adaptive/ic_launcher_monochrome.png")
    for density, px in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
        save(render(icon_svg("rounded"), px), f"android/mipmap-{density}/ic_launcher.png")
        save(render(icon_svg("circle"), px), f"android/mipmap-{density}/ic_launcher_round.png")

    # General logo files
    for px in (1024, 512, 256, 128):
        save(render(icon_svg(), px), f"logo/icon-{px}.png")
    for px in (1024, 512, 256):
        save(render(icon_svg("circle"), px), f"logo/icon-round-{px}.png")
        save(mark_png(px), f"logo/mark-{px}.png")
        save(render(mono_svg(WHITE), px, round(px * 48 / 64)), f"logo/mark-white-{px}.png")
    save(render(lockup_svg(dark=True), 1120, 320), "logo/lockup-dark.png", opaque=True)
    save(render(lockup_svg(dark=False), 1120, 320), "logo/lockup-light.png")

    # Web: favicons and PWA icons for the GitHub Pages site
    for px in (16, 32, 48, 192, 512):
        save(render(icon_svg("circle"), px), f"web/favicon-{px}.png")
    save(render(icon_svg(), 180), "web/apple-touch-icon.png", opaque=True)
    render(icon_svg("circle"), 256).save(
        os.path.join(BRAND, "web/favicon.ico"), sizes=[(16, 16), (32, 32), (48, 48)])
    write("web/favicon.svg", icon_svg("circle"))

    # App launcher drawables
    drawable = os.path.join(RES, "drawable")
    for name, xml in [("ic_launcher_background.xml", vd_background()),
                      ("ic_launcher_foreground.xml", vd_foreground()),
                      ("ic_launcher_monochrome.xml", vd_monochrome())]:
        with open(os.path.join(drawable, name), "w") as f:
            f.write(xml)


if __name__ == "__main__":
    main()
