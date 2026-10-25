"""生成明确标注的演示商品、逐型号资料与本地SVG示意图；不连接数据库。"""
import json
from pathlib import Path
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parents[1]
VERSION = "2026-10-25.1"


def build():
    rows = []
    def add(category, model, name, brand, price, sections, topics, stock=20):
        key = f"demo-{category}-{model.lower()}"
        title = f"{brand.replace('演示', '')} {model} {name}"
        rows.append({"key": key, "category": category, "model": model, "title": title,
                     "brand": brand, "price": f"{price:.2f}", "initialStock": stock,
                     "imageUrl": f"/shopagent/catalog/{model.lower()}.svg",
                     "description": "虚构演示商品，不代表真实品牌或性能承诺。" + " ".join(sections.values()),
                     "aliases": [model, title, title.replace(" ", "")], "version": VERSION,
                     "source": f"catalog/{key}.md", "sections": sections, "topics": topics})
    audio = [
        ("E01", "轻巧蓝牙耳机", 129, "蓝牙5.3，单设备连接，入耳式；不支持主动降噪。", "USB-C充电，不支持无线充电；单次约6小时，配盒约24小时。"),
        ("E02", "通勤降噪耳机", 299, "蓝牙5.3，支持双设备切换与主动降噪，入耳式。", "USB-C或无线充电；单次约7小时，配盒约28小时。"),
        ("E03", "有线入耳耳机", 69, "3.5毫米音频插头，带线控麦克风；不支持蓝牙与主动降噪。", "有线供电，无内置电池，无需充电。"),
        ("E04", "USB会议耳麦", 159, "USB-A有线连接，头戴式，带可调麦克风；不支持蓝牙。", "电脑USB供电，不含电池，无需充电。"),
        ("E05", "开放式蓝牙耳机", 239, "蓝牙5.3，开放式耳挂，单设备；不支持主动降噪。", "USB-C充电，不支持无线充电；单次约10小时。"),
        ("E06", "运动耳挂耳机", 199, "蓝牙5.2，入耳式耳挂，单设备；不支持主动降噪。", "USB-C充电，不支持无线充电；单次约8小时。"),
        ("E07", "USB-C数字耳机", 89, "USB-C数字音频有线连接，设备必须支持USB音频；不支持蓝牙。", "由连接设备供电，无内置电池，无需充电。"),
        ("E08", "头戴蓝牙耳机", 459, "蓝牙5.3，头戴式，支持双设备与3.5毫米有线连接。", "USB-C充电，不支持无线充电；蓝牙模式约40小时。"),
    ]
    for model, name, price, connection, power in audio:
        add("audio", model, name, "澄声演示", price,
            {"连接与兼容": connection, "供电与续航": power,
             "使用与维护": "耳机使用后用柔软干布清洁，不浸水；资料未提供防水等级或检测报告。续航为演示设定，实际使用条件未建模。",
             "包装与限制": "包装包含耳机、使用说明；需要充电的型号附对应USB线，有线型号不另附充电线；均不含充电头。不提供未记载的编码、延迟与保修期限。"},
            ["蓝牙", "连接", "兼容", "设备", "有线", "降噪", "接口", "充电", "无线充", "供电", "电池", "续航", "小时", "清洁", "保养", "防水", "包装", "配件", "充电头"])
    shoes = [
        ("R01", "轻量慢跑鞋", 229, "标准楦，尺码36至44，日常慢跑与通勤。", "网布鞋面，柔和中底，不含碳板。"),
        ("R02", "宽楦慢跑鞋", 329, "宽楦，尺码38至46，适合偏宽脚型的日常慢跑。", "网布鞋面，缓震中底，不含碳板。"),
        ("R03", "缓震长跑鞋", 459, "标准楦，尺码36至45，日常长距离训练。", "织物鞋面，加厚缓震中底，不含碳板。"),
        ("R04", "综合训练鞋", 299, "标准楦，尺码36至45，室内综合训练；不是专业竞速跑鞋。", "织物鞋面，稳定底台与橡胶外底。"),
        ("R05", "公路竞速鞋", 699, "偏窄楦，尺码38至44，公路速度训练。", "轻薄鞋面，弹性中底，演示设定含尼龙支撑片，不含碳板。"),
        ("R06", "步行休闲鞋", 189, "标准楦，尺码35至44，城市步行与通勤。", "织物鞋面，柔软中底，不作为专业运动防护。"),
        ("R07", "入门跑鞋", 159, "标准楦，尺码36至44，短距离入门慢跑。", "网布鞋面，基础缓震，不含碳板。"),
        ("R08", "通勤防泼水鞋", 379, "标准楦，尺码37至45，城市步行通勤。", "合成鞋面，表面防泼水；不等于防水鞋，不适合浸水。"),
    ]
    for model, name, price, fit, material in shoes:
        add("shoes", model, name, "步禾演示", price,
            {"尺码与用途": fit, "材质与结构": material,
             "清洁与维护": "用软刷清除表面污物后阴干，不机洗、不高温烘干；不同脚型需试穿，资料没有重量或检测等级。",
             "包装与限制": "包装包含一双鞋和说明卡；没有额外鞋垫或护理剂。仅演示参数，不提供医疗矫正或受伤预防承诺。"},
            ["尺码", "脚型", "宽楦", "窄", "通勤", "慢跑", "训练", "步行", "材质", "鞋面", "中底", "碳板", "防水", "防泼", "清洁", "机洗", "烘干", "包装", "鞋垫"])
    keyboards = [
        ("K01", "紧凑有线键盘", 129, "68键，USB-C有线连接；支持Windows与macOS基础输入。", "连接设备USB供电，无电池，无需充电。"),
        ("K02", "三模机械键盘", 299, "75%配列，USB-C有线、蓝牙、2.4GHz三模；Windows与macOS基础输入。", "4000mAh电池，USB-C充电；不支持无线充电。"),
        ("K03", "全尺寸办公键盘", 159, "104键，带独立数字区，USB-A有线；Windows与macOS基础输入。", "USB供电，无电池，无需充电。"),
        ("K04", "静音蓝牙键盘", 239, "87键，蓝牙连接，最多记忆3台设备并手动切换。", "两节AAA电池供电；不支持USB充电，包装不含电池。"),
        ("K05", "数字区三模键盘", 399, "98键配列，带数字区；USB-C、蓝牙、2.4GHz三模，支持两系统基础输入。", "5000mAh电池，USB-C充电；不支持无线充电。"),
        ("K06", "便携薄膜键盘", 89, "78键薄膜，USB-C有线；支持两系统基础输入，不含独立数字区。", "连接设备供电，无电池，无需充电。"),
    ]
    for model, name, price, connection, power in keyboards:
        add("keyboard", model, name, "键序演示", price,
            {"配列与连接": connection, "供电与充电": power,
             "使用与维护": "断开供电后用干布清洁，不浸水；功能键映射可能随系统不同，资料没有承诺特定驱动或游戏延迟。",
             "包装与限制": "包装含键盘和说明；有线/可充电型号附USB线，三模型号另含2.4GHz接收器。资料未承诺热插拔、轴体寿命或防水等级。"},
            ["配列", "键", "数字", "连接", "蓝牙", "三模", "2.4", "有线", "系统", "兼容", "windows", "mac", "供电", "充电", "电池", "清洁", "防水", "包装", "接收器"])
    mice = [
        ("M01", "轻巧有线鼠标", 59, "USB-A有线连接，最高3200DPI，四档手动切换。", "USB供电，无电池。"),
        ("M02", "双模办公鼠标", 129, "蓝牙与2.4GHz双模，最高2400DPI，可切换两台设备。", "一节AA电池供电，不支持USB充电；不含电池。"),
        ("M03", "充电静音鼠标", 169, "蓝牙与2.4GHz双模，静音主按键，最高4000DPI。", "USB-C充电，500mAh内置电池；不支持无线充电。"),
        ("M04", "竖握办公鼠标", 199, "蓝牙与2.4GHz双模，竖握外形，最高3200DPI。", "USB-C充电，600mAh内置电池；不支持无线充电。"),
        ("M05", "轻量游戏鼠标", 299, "USB-C有线与2.4GHz连接，最高16000DPI，演示重量约65克。", "USB-C充电，400mAh内置电池；不支持无线充电。"),
        ("M06", "便携蓝牙鼠标", 99, "仅蓝牙连接，最高1600DPI；不提供2.4GHz模式。", "一节AA电池供电，不支持USB充电；不含电池。"),
    ]
    for model, name, price, connection, power in mice:
        add("mouse", model, name, "轻点演示", price,
            {"连接与参数": connection, "供电与充电": power,
             "使用与维护": "适合平整非透明表面，不承诺在玻璃上工作；用干布清洁，不浸水。",
             "包装与限制": "包装包含鼠标和说明；2.4GHz型号含接收器，可充电型号含USB线。Windows与macOS基础指针输入，不承诺特定驱动、回报率或医疗作用。"},
            ["连接", "蓝牙", "2.4", "有线", "dpi", "档", "重量", "克", "静音", "供电", "充电", "电池", "接收器", "系统", "兼容", "玻璃", "表面", "清洁", "包装"])
    for model, name, price, capacity, laptop, material in [
        ("B01", "轻便日用背包", 129, 12, "可放13英寸设备，不含独立电脑夹层。", "聚酯纤维"),
        ("B02", "通勤电脑背包", 239, 18, "独立电脑夹层最大14英寸，具体仍需核对设备尺寸。", "600D聚酯纤维"),
        ("B03", "大容量通勤背包", 329, 22, "独立电脑夹层最大15.6英寸，具体仍需核对设备尺寸。", "尼龙织物"),
        ("B04", "周末轻旅行背包", 299, 28, "没有独立电脑夹层，适合衣物与日用品。", "600D聚酯纤维"),
    ]:
        add("bag", model, name, "行简演示", price,
            {"容量与收纳": f"标称容量{capacity}升。{laptop}", "材质与背负": f"主要面料为{material}，双肩背负，肩带可调。",
             "清洁与限制": "用湿布局部擦拭后阴干，不机洗；不承诺防水等级或防盗功能，不适合浸水。",
             "包装与配件": "包装包含背包与说明卡，不含雨罩、收纳袋或电脑。所有容量为演示设定。"},
            ["容量", "升", "电脑", "英寸", "夹层", "收纳", "材质", "面料", "肩带", "背负", "清洁", "机洗", "防水", "雨罩", "包装"])
    for model, name, price, capacity, material, usage in [
        ("C01", "随行保温杯", 89, 350, "304不锈钢内胆", "适合温水；演示保温设定为6小时，不支持微波炉。"),
        ("C02", "通勤保温杯", 129, 450, "316不锈钢内胆", "适合温水；演示保温设定为8小时，不支持微波炉。"),
        ("C03", "玻璃随行杯", 69, 600, "硼硅玻璃杯体与PP杯盖", "不是保温杯，不提供保温时长；避免冷热骤变，不支持微波炉。"),
        ("C04", "运动冷水杯", 59, 800, "PP杯体", "仅冷水或常温水，不装沸水；不是保温杯，不支持微波炉。"),
    ]:
        add("cup", model, name, "温序演示", price,
            {"容量与材质": f"标称容量{capacity}毫升，{material}。", "温度与使用": usage,
             "清洁与限制": "建议手洗，不使用洗碗机；杯盖拆下后清洁并充分晾干。资料未提供检测认证或密封耐压承诺。",
             "包装与配件": "包装包含杯体、杯盖与说明卡，不含杯刷或吸管；材质和保温时长为虚构演示参数。"},
            ["容量", "毫升", "材质", "内胆", "不锈钢", "玻璃", "保温", "小时", "温度", "冷水", "沸水", "微波", "清洁", "手洗", "洗碗", "包装", "吸管", "杯刷"])
    target = ROOT / "ai-service/knowledge/catalog"
    target.mkdir(parents=True, exist_ok=True)
    assets = ROOT / "backend/src/main/resources/static/shopagent/catalog"
    assets.mkdir(parents=True, exist_ok=True)
    colors = {"audio": "#dce9ff", "shoes": "#ffe2cf", "keyboard": "#e4e1ff", "mouse": "#d9f0e8", "bag": "#f8e4bd", "cup": "#d5edf2"}
    symbols = {
        "audio": '<path d="M180 210v-25a76 76 0 0 1 152 0v25"/><rect x="165" y="197" width="40" height="72" rx="16"/><rect x="307" y="197" width="40" height="72" rx="16"/>',
        "shoes": '<path d="m152 225 31-72 42 19 18 44 96 30 24 30H145v-32Z"/><path d="m213 204 40-8m-27 26 43-7M147 260h210"/>',
        "keyboard": '<rect x="125" y="164" width="262" height="134" rx="18"/><path d="M149 191h213M149 218h213M149 245h213M172 187v64m42-64v64m42-64v64m42-64v64m42-64v64m-150 27h132"/>',
        "mouse": '<rect x="190" y="139" width="132" height="168" rx="64"/><path d="M256 141v72m-63 0h126"/><rect x="247" y="164" width="18" height="32" rx="8"/>',
        "bag": '<path d="M222 145v-20h68v20"/><rect x="169" y="146" width="174" height="184" rx="34"/><rect x="194" y="236" width="124" height="68" rx="16"/><path d="M172 192h168"/>',
        "cup": '<path d="M201 146h110l-14 171h-82Z"/><rect x="193" y="129" width="126" height="27" rx="9"/><path d="M312 173h28a29 29 0 0 1 0 58h-32"/>',
    }
    for item in rows:
        text = f"# {item['title']} 使用资料\n\n> 虚构演示资料，不代表真实商品承诺。版本：{VERSION}。\n"
        for section, content in item["sections"].items():
            text += f"\n## {section}\n{content}\n"
        (ROOT / "ai-service/knowledge" / item["source"]).write_text(text, encoding="utf-8", newline="\n")
        svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="512" height="420" viewBox="0 0 512 420" role="img" aria-label="{escape(item['title'])}演示示意图"><rect width="512" height="420" rx="28" fill="{colors[item['category']]}"/><g fill="none" stroke="#24384a" stroke-width="9" stroke-linecap="round" stroke-linejoin="round">{symbols[item['category']]}</g><text x="30" y="48" font-family="sans-serif" font-size="22" fill="#24384a">SHOPAGENT / {item['model']}</text><text x="30" y="387" font-family="sans-serif" font-size="19" fill="#24384a">虚构商品 · 演示示意图</text></svg>'''
        (assets / f"{item['model'].lower()}.svg").write_text(svg, encoding="utf-8", newline="\n")
    legacy = {"key": "demo-earphone-k1", "title": "演示耳机 K1", "brand": "独立教学样例",
              "aliases": ["K1", "演示耳机K1", "Demo-K1"], "version": "legacy-1", "source": "demo_earphone_guide.md",
              "topics": ["连接", "蓝牙", "充电", "接口", "无线充", "指示灯", "包装", "配件", "充电头"]}
    manifest = {"version": VERSION, "notice": "全部为虚构演示数据；原有商品和库存不覆盖。", "products": rows, "legacy_documents": [legacy]}
    path = ROOT / "ai-service/knowledge/catalog.json"
    path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(json.dumps({"new_products": len(rows), "categories": len(colors), "documents": len(rows) + 1, "assets": len(rows)}, ensure_ascii=False))


if __name__ == "__main__":
    build()
