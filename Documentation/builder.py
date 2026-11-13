#!/usr/bin/env python3
"""
SkillQuest Platform Architectural & UI/UX Design Specification Generator
Generates a publication-quality PDF document using PostScript compilation.
Strictly adheres to technology-neutral architectural descriptions.
"""

import os
import subprocess
import sys

def escape_ps(text):
    """Escape special characters for PostScript strings."""
    return text.replace('\\', '\\\\').replace('(', '\\(').replace(')', '\\)')

class DocumentBuilder:
    def __init__(self, filename="/tmp/SkillQuest_Specification.ps"):
        self.filename = filename
        self.pages = []
        self.current_page = []
        self.page_num = 0
        self.width = 595  # A4 width in points
        self.height = 842 # A4 height in points
        self.margin_x = 45
        self.margin_top = 780
        self.margin_bottom = 50
        self.cursor_y = self.margin_top

    def new_page(self):
        if self.current_page:
            self.pages.append(self.current_page)
        self.current_page = []
        self.page_num += 1
        self.cursor_y = self.margin_top

    def add_raw(self, ps_code):
        self.current_page.append(ps_code)

    def set_color(self, r, g, b):
        self.add_raw(f"{r:.3f} {g:.3f} {b:.3f} setrgbcolor")

    def draw_rect(self, x, y, w, h, fill=True, r=0, g=0, b=0):
        self.set_color(r, g, b)
        self.add_raw(f"newpath {x} {y} {w} {h} rectfill" if fill else f"newpath {x} {y} {w} {h} rectstroke")

    def draw_rounded_rect(self, x, y, w, h, radius, fill=True, r=0.9, g=0.9, b=0.9):
        self.set_color(r, g, b)
        code = f"""
        newpath
        {x + radius} {y} moveto
        {x + w} {y} {x + w} {y + h} {radius} arcto 4 {{pop}} repeat
        {x + w} {y + h} {x} {y + h} {radius} arcto 4 {{pop}} repeat
        {x} {y + h} {x} {y} {radius} arcto 4 {{pop}} repeat
        {x} {y} {x + w} {y} {radius} arcto 4 {{pop}} repeat
        closepath
        {"fill" if fill else "stroke"}
        """
        self.add_raw(code)

    def draw_line(self, x1, y1, x2, y2, width=1, r=0.7, g=0.7, b=0.7):
        self.set_color(r, g, b)
        self.add_raw(f"{width} setlinewidth newpath {x1} {y1} moveto {x2} {y2} lineto stroke")

    def draw_text(self, text, x, y, font="Helvetica", size=10, r=0.1, g=0.1, b=0.1):
        self.set_color(r, g, b)
        safe = escape_ps(text)
        self.add_raw(f"/{font} findfont {size} scalefont setfont {x} {y} moveto ({safe}) show")

    def draw_header_footer(self, total_pages, p_num):
        # Header
        self.draw_text("SKILLQUEST PLATFORM SPECIFICATION", self.margin_x, 810, "Helvetica-Bold", 8, 0.4, 0.45, 0.55)
        self.draw_text("SYSTEM ARCHITECTURE & UI/UX SPECIFICATION", 320, 810, "Helvetica", 8, 0.4, 0.45, 0.55)
        self.draw_line(self.margin_x, 804, self.width - self.margin_x, 804, 0.75, 0.8, 0.82, 0.88)

        # Footer
        self.draw_line(self.margin_x, 42, self.width - self.margin_x, 42, 0.75, 0.8, 0.82, 0.88)
        self.draw_text("CONFIDENTIAL - EDUCATIONAL & ENTERPRISE ARCHITECTURE", self.margin_x, 30, "Helvetica", 7.5, 0.5, 0.5, 0.5)
        page_str = f"Page {p_num} of {total_pages}"
        self.draw_text(page_str, self.width - self.margin_x - 55, 30, "Helvetica-Bold", 7.5, 0.3, 0.35, 0.45)

    def ensure_space(self, required_pts):
        if self.cursor_y - required_pts < self.margin_bottom:
            self.new_page()

    def add_title_banner(self, title, subtitle, badge="SYSTEM WHITE PAPER"):
        self.ensure_space(110)
        y = self.cursor_y - 85
        # Background dark gradient/card
        self.draw_rounded_rect(self.margin_x, y, 505, 85, 6, fill=True, r=0.06, g=0.09, b=0.16)
        # Accent left bar
        self.draw_rounded_rect(self.margin_x, y, 6, 85, 3, fill=True, r=0.15, g=0.45, b=0.95)
        
        # Badge
        self.draw_rounded_rect(self.margin_x + 18, y + 60, 120, 15, 3, fill=True, r=0.15, g=0.25, b=0.45)
        self.draw_text(badge, self.margin_x + 24, y + 64, "Helvetica-Bold", 7, 0.6, 0.8, 1.0)

        # Title
        self.draw_text(title, self.margin_x + 18, y + 36, "Helvetica-Bold", 17, 1.0, 1.0, 1.0)
        # Subtitle
        self.draw_text(subtitle, self.margin_x + 18, y + 18, "Helvetica", 9, 0.75, 0.82, 0.92)

        self.cursor_y = y - 18

    def add_section_header(self, title, icon=">"):
        self.ensure_space(45)
        y = self.cursor_y - 26
        # Header background
        self.draw_rounded_rect(self.margin_x, y, 505, 23, 4, fill=True, r=0.93, g=0.95, b=0.98)
        self.draw_line(self.margin_x, y, self.margin_x + 4, y + 23, 4, 0.15, 0.45, 0.95)
        self.draw_text(f"{icon}  {title.upper()}", self.margin_x + 12, y + 7, "Helvetica-Bold", 10, 0.1, 0.15, 0.3)
        self.cursor_y = y - 12

    def add_subsection_header(self, title):
        self.ensure_space(25)
        y = self.cursor_y - 18
        self.draw_text(title, self.margin_x + 2, y + 3, "Helvetica-Bold", 9.5, 0.15, 0.25, 0.45)
        self.draw_line(self.margin_x + 2, y, self.margin_x + 160, y, 1, 0.2, 0.5, 0.9)
        self.cursor_y = y - 10

    def add_paragraph(self, text, font="Helvetica", size=8.5, color=(0.2, 0.22, 0.25), line_height=12.5):
        words = text.split()
        lines = []
        current_line = []
        max_chars_per_line = int(505 / (size * 0.52))

        for w in words:
            test_line = " ".join(current_line + [w])
            if len(test_line) <= max_chars_per_line:
                current_line.append(w)
            else:
                lines.append(" ".join(current_line))
                current_line = [w]
        if current_line:
            lines.append(" ".join(current_line))

        needed = len(lines) * line_height + 4
        self.ensure_space(needed)

        for l in lines:
            self.cursor_y -= line_height
            self.draw_text(l, self.margin_x + 2, self.cursor_y, font, size, *color)
        self.cursor_y -= 4

    def add_callout(self, title, text, r_bg=0.95, g_bg=0.97, b_bg=1.0, r_border=0.2, g_border=0.45, b_border=0.95):
        words = text.split()
        lines = []
        current_line = []
        max_chars_per_line = 90
        for w in words:
            test_line = " ".join(current_line + [w])
            if len(test_line) <= max_chars_per_line:
                current_line.append(w)
            else:
                lines.append(" ".join(current_line))
                current_line = [w]
        if current_line:
            lines.append(" ".join(current_line))

        height = len(lines) * 11 + 28
        self.ensure_space(height + 10)
        y = self.cursor_y - height

        self.draw_rounded_rect(self.margin_x, y, 505, height, 4, fill=True, r=r_bg, g=g_bg, b=b_bg)
        self.draw_rounded_rect(self.margin_x, y, 4, height, 2, fill=True, r=r_border, g=g_border, b=b_border)
        self.draw_text(title, self.margin_x + 12, y + height - 14, "Helvetica-Bold", 8.5, r_border*0.7, g_border*0.7, b_border*0.7)
        ty = y + height - 26
        for l in lines:
            self.draw_text(l, self.margin_x + 12, ty, "Helvetica", 7.5, 0.2, 0.25, 0.3)
            ty -= 11

        self.cursor_y = y - 8

    def add_bullet(self, bold_prefix, text):
        full_text = bold_prefix + " " + text
        words = full_text.split()
        max_chars = 95

        lines = []
        cur = []
        for w in words:
            if len(" ".join(cur + [w])) <= max_chars:
                cur.append(w)
            else:
                lines.append(" ".join(cur))
                cur = [w]
        if cur:
            lines.append(" ".join(cur))

        self.ensure_space(len(lines) * 12 + 4)

        for i, l in enumerate(lines):
            self.cursor_y -= 12
            if i == 0:
                self.draw_rounded_rect(self.margin_x + 6, self.cursor_y + 2.5, 4, 4, 1, fill=True, r=0.2, g=0.5, b=0.9)
                if l.startswith(bold_prefix):
                    rem = l[len(bold_prefix):].strip()
                    self.draw_text(bold_prefix, self.margin_x + 16, self.cursor_y, "Helvetica-Bold", 8, 0.1, 0.15, 0.25)
                    offset = len(bold_prefix) * 4.6
                    self.draw_text(rem, self.margin_x + 16 + offset, self.cursor_y, "Helvetica", 8, 0.25, 0.28, 0.32)
                else:
                    self.draw_text(l, self.margin_x + 16, self.cursor_y, "Helvetica", 8, 0.25, 0.28, 0.32)
            else:
                self.draw_text(l, self.margin_x + 16, self.cursor_y, "Helvetica", 8, 0.25, 0.28, 0.32)
        self.cursor_y -= 2

    def add_card_grid(self, cards):
        count = len(cards)
        if count == 2:
            card_w = 246
            gap = 13
        elif count == 3:
            card_w = 160
            gap = 12
        else:
            card_w = 505
            gap = 0

        max_lines = 0
        card_lines = []
        for c in cards:
            words = c['desc'].split()
            lines = []
            cur = []
            max_c = int(card_w / 4.4)
            for w in words:
                if len(" ".join(cur + [w])) <= max_c:
                    cur.append(w)
                else:
                    lines.append(" ".join(cur))
                    cur = [w]
            if cur:
                lines.append(" ".join(cur))
            card_lines.append(lines)
            if len(lines) > max_lines:
                max_lines = len(lines)

        card_h = max_lines * 10 + 38
        self.ensure_space(card_h + 10)
        y = self.cursor_y - card_h

        for i, c in enumerate(cards):
            cx = self.margin_x + i * (card_w + gap)
            cr, cg, cb = c.get('color', (0.15, 0.45, 0.95))
            self.draw_rounded_rect(cx, y, card_w, card_h, 4, fill=True, r=0.97, g=0.98, b=0.99)
            self.draw_rounded_rect(cx, y, card_w, card_h, 4, fill=False, r=0.88, g=0.90, b=0.94)
            self.draw_rounded_rect(cx, y + card_h - 3, card_w, 3, 1, fill=True, r=cr, g=cg, b=cb)

            tag = c.get('tag', 'MODULE')
            self.draw_text(tag.upper(), cx + 8, y + card_h - 13, "Helvetica-Bold", 6.5, cr, cg, cb)
            self.draw_text(c['title'], cx + 8, y + card_h - 24, "Helvetica-Bold", 8.5, 0.1, 0.15, 0.25)

            ty = y + card_h - 35
            for l in card_lines[i]:
                self.draw_text(l, cx + 8, ty, "Helvetica", 7.2, 0.3, 0.35, 0.4)
                ty -= 9.5

        self.cursor_y = y - 10

    def compile(self, output_pdf_path):
        if self.current_page:
            self.pages.append(self.current_page)

        total_pages = len(self.pages)
        print(f"Compiling {total_pages} pages into PostScript...")

        with open(self.filename, "w") as f:
            f.write("%!PS-Adobe-3.0\n")
            f.write(f"%%Pages: {total_pages}\n")
            f.write(f"%%BoundingBox: 0 0 {self.width} {self.height}\n")
            f.write("%%EndComments\n\n")

            for idx, p_ops in enumerate(self.pages):
                f.write(f"%%Page: {idx + 1} {idx + 1}\n")
                # Draw header and footer
                for hf_op in self._generate_header_footer(total_pages, idx + 1):
                    f.write(hf_op + "\n")
                # Write page content
                for op in p_ops:
                    f.write(op + "\n")
                f.write("showpage\n\n")
            f.write("%%EOF\n")

        print(f"Converting {self.filename} to {output_pdf_path} via ps2pdf...")
        cmd = ["ps2pdf", "-dPDFSETTINGS=/prepress", self.filename, output_pdf_path]
        res = subprocess.run(cmd, capture_output=True, text=True)
        if res.returncode != 0:
            print("ps2pdf error:", res.stderr)
            raise RuntimeError(f"ps2pdf failed: {res.stderr}")
        print(f"Successfully generated: {output_pdf_path} ({os.path.getsize(output_pdf_path)} bytes)")

    def _generate_header_footer(self, total_pages, p_num):
        ops = []
        # Header
        ops.append("0.400 0.450 0.550 setrgbcolor /Helvetica-Bold findfont 8 scalefont setfont 45 810 moveto (SKILLQUEST PLATFORM SPECIFICATION) show")
        ops.append("0.400 0.450 0.550 setrgbcolor /Helvetica findfont 8 scalefont setfont 320 810 moveto (SYSTEM ARCHITECTURE & UI/UX SPECIFICATION) show")
        ops.append("0.800 0.820 0.880 setrgbcolor 0.75 setlinewidth newpath 45 804 moveto 550 804 lineto stroke")

        # Footer
        ops.append("0.800 0.820 0.880 setrgbcolor 0.75 setlinewidth newpath 45 42 moveto 550 42 lineto stroke")
        ops.append("0.500 0.500 0.500 setrgbcolor /Helvetica findfont 7.5 scalefont setfont 45 30 moveto (CONFIDENTIAL - EDUCATIONAL & ENTERPRISE ARCHITECTURE) show")
        page_str = escape_ps(f"Page {p_num} of {total_pages}")
        ops.append(f"0.300 0.350 0.450 setrgbcolor /Helvetica-Bold findfont 7.5 scalefont setfont 495 30 moveto ({page_str}) show")
        return ops

Path = "/home/sabriii/WEB_dist/Documentation/builder.py"
