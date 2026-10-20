#!/usr/bin/env python3
"""Rebuild G002's six-sheet rough-balance PAR using only Python's standard library."""
import json
from pathlib import Path
from xml.sax.saxutils import escape
from zipfile import ZipFile, ZIP_DEFLATED

ROOT = Path(__file__).resolve().parent
def column(index):
    result = ""
    while index:
        index, digit = divmod(index - 1, 26)
        result = chr(65 + digit) + result
    return result

def sheet_xml(rows):
    cells = []
    for row_number, row in enumerate(rows, 1):
        values = []
        for index, value in enumerate(row, 1):
            if value is None or value == "":
                continue
            ref = f"{column(index)}{row_number}"
            if isinstance(value, (int, float)):
                values.append(f'<c r="{ref}"><v>{value}</v></c>')
            else:
                values.append(f'<c r="{ref}" t="inlineStr"><is><t>{escape(str(value))}</t></is></c>')
        cells.append(f'<row r="{row_number}">{"".join(values)}</row>')
    return '<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>' + "".join(cells) + '</sheetData></worksheet>'

def build(data, output):
    names = ["COAL", "COPPER", "IRON", "SILVER", "GOLD", "EMBER", "WILD", "SCATTER"]
    sheets = []
    for mode, cost in data["costs"].items():
        denominator = data["triggerDenominator"]
        trigger = denominator if mode == "BB1" else 4 if mode == "EM1" else 1
        config = [["Game-Wide-Config"], ["screen", "5x3"], ["cost", cost],
                  ["triggerDenominator", denominator], ["freeSpins", 8], ["maxHeat", 5],
                  ["maxWin", data["maxWin"]], [],
                  ["PAYTABLE"], ["Symbol-ID", "Symbol-Name", "Type", "3oak", "4oak", "5oak"]]
        for i, name in enumerate(names, 1):
            config.append([i, name, "SIMPLE" if i <= 6 else "WILD" if i == 7 else "FEATURE",
                           *(data["payouts"][i - 1] if i <= 6 else [0, 0, 0])])
        config += [[], ["PAYLINES"], ["Line", "R1", "R2", "R3", "R4", "R5"]]
        config += [[i, *line] for i, line in enumerate(data["lines"], 1)]
        config += [[], ["CHANCETABLE:BG-setSelection"], ["Value", "Weight"],
                   [0, denominator - trigger], [1, trigger], [],
                   ["CHANCETABLE:FG-setSelection"], ["Value", "Weight"], [0, 1], [],
                   ["MAP:SCATTER-FREEGAMES"], ["Scatters", "Freegames"], [3, 8]]
        groups = [data["baseReels"], data["entryReels"], data["freeReels"]]
        reels = [["BG-0", None, None, None, None, None,
                  "BG-1", None, None, None, None, None, "FG-0"],
                 [1, 2, 3, 4, 5, None, 1, 2, 3, 4, 5, None, 1, 2, 3, 4, 5]]
        for row in range(max(len(strip) for group in groups for strip in group)):
            values = []
            for index, group in enumerate(groups):
                if index:
                    values.append(None)
                values.extend(strip[row] if row < len(strip) else None for strip in group)
            reels.append(values)
        sheets.extend([(mode + "-Config", config), (mode + "-Reels", reels)])
    main = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    rel = "http://schemas.openxmlformats.org/package/2006/relationships"
    with ZipFile(output, "w", ZIP_DEFLATED) as z:
        overrides = "".join(f'<Override PartName="/xl/worksheets/sheet{i}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>' for i in range(1, 7))
        z.writestr("[Content_Types].xml", '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>' + overrides + '</Types>')
        z.writestr("_rels/.rels", f'<Relationships xmlns="{rel}"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>')
        titles = "".join(f'<sheet name="{name}" sheetId="{i}" r:id="rId{i}"/>' for i, (name, _) in enumerate(sheets, 1))
        z.writestr("xl/workbook.xml", f'<workbook xmlns="{main}" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>{titles}</sheets></workbook>')
        relations = "".join(f'<Relationship Id="rId{i}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet{i}.xml"/>' for i in range(1, 7))
        z.writestr("xl/_rels/workbook.xml.rels", f'<Relationships xmlns="{rel}">{relations}</Relationships>')
        for i, (_, rows) in enumerate(sheets, 1):
            z.writestr(f"xl/worksheets/sheet{i}.xml", sheet_xml(rows))

if __name__ == "__main__":
    build(json.loads((ROOT / "balance-input.json").read_text()), ROOT / "PAR.xlsx")
    print("Rebuilt G002 PAR.xlsx from balance-input.json")
