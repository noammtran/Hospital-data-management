"""
Generate a flowchart-style UML class diagram (boxes + arrows) and insert it
as a new UMLModel into Diagram.mdj, without touching existing content.

Test run: only the uuid-generation diagram (5 boxes, 4 arrows).
Boxes are UMLClass views with suppressed attribute/operation compartments,
so they render as plain labelled boxes. Arrows are UMLDependency views.
"""
import json, sys

MDJ = "Diagram.mdj"
PROJECT_ID = "AAAAAAFF+h6SjaM2Hec="

def counter():
    n = 0
    while True:
        n += 1
        yield n

def name_compartment(box_id, model_id, prefix, L, T, W, text):
    ncv_id = f"{prefix}_ncv"
    labels = []
    # 0 stereotype (hidden), 1 name (visible), 2 namespace (hidden), 3 property (hidden)
    lbl_ids = [f"{prefix}_lblA", f"{prefix}_lblB", f"{prefix}_lblC", f"{prefix}_lblD"]
    for i, lid in enumerate(lbl_ids):
        lbl = {
            "_type": "LabelView", "_id": lid,
            "_parent": {"$ref": ncv_id},
            "font": "Arial;13;1" if i == 1 else "Arial;13;0",
            "parentStyle": True, "height": 13,
        }
        if i == 1:
            lbl.update({"left": L + 10, "top": T + 13, "width": W - 20,
                        "text": text, "wordWrap": True})
        else:
            lbl.update({"visible": False, "left": L, "top": T, "width": 1})
        labels.append(lbl)
    return {
        "_type": "UMLNameCompartmentView", "_id": ncv_id,
        "_parent": {"$ref": box_id}, "model": {"$ref": model_id},
        "subViews": labels,
        "font": "Arial;13;0", "parentStyle": True,
        "left": L, "top": T, "width": W, "height": 40,
        "stereotypeLabel": {"$ref": lbl_ids[0]},
        "nameLabel": {"$ref": lbl_ids[1]},
        "namespaceLabel": {"$ref": lbl_ids[2]},
        "propertyLabel": {"$ref": lbl_ids[3]},
    }

def hidden_compartment(t, box_id, model_id, cid, L, T):
    return {
        "_type": t, "_id": cid, "_parent": {"$ref": box_id},
        "model": {"$ref": model_id}, "visible": False,
        "font": "Arial;13;0", "parentStyle": True,
        "left": L, "top": T, "width": 10, "height": 10,
    }

def class_view(box_id, model_id, prefix, dgm_id, L, T, W, H, text):
    ncv = name_compartment(box_id, model_id, prefix, L, T, W, text)
    acv = hidden_compartment("UMLAttributeCompartmentView", box_id, model_id, f"{prefix}_acv", L, T)
    ocv = hidden_compartment("UMLOperationCompartmentView", box_id, model_id, f"{prefix}_ocv", L, T)
    rcv = hidden_compartment("UMLReceptionCompartmentView", box_id, model_id, f"{prefix}_rcv", L, T)
    tcv = hidden_compartment("UMLTemplateParameterCompartmentView", box_id, model_id, f"{prefix}_tcv", L, T)
    return {
        "_type": "UMLClassView", "_id": box_id,
        "_parent": {"$ref": dgm_id}, "model": {"$ref": model_id},
        "subViews": [ncv, acv, ocv, rcv, tcv],
        "font": "Arial;13;0", "parentStyle": False, "containerChangeable": True,
        "left": L, "top": T, "width": W, "height": H,
        "nameCompartment": {"$ref": ncv["_id"]},
        "suppressAttributes": True, "suppressOperations": True,
        "attributeCompartment": {"$ref": acv["_id"]},
        "operationCompartment": {"$ref": ocv["_id"]},
        "receptionCompartment": {"$ref": rcv["_id"]},
        "templateParameterCompartment": {"$ref": tcv["_id"]},
    }

def dependency_view(dpv_id, model_id, dgm_id, tail_view, head_view, points):
    elv = []
    for s in ("A", "B", "C"):
        elv.append({
            "_type": "EdgeLabelView", "_id": f"{dpv_id}_el{s}",
            "_parent": {"$ref": dpv_id}, "model": {"$ref": model_id},
            "visible": False, "lineColor": "#333333", "font": "Arial;13;0",
            "parentStyle": True, "left": 0, "top": 0, "width": 80, "height": 13,
            "alpha": 1.5708, "distance": 15, "edgePosition": 1,
        })
    return {
        "_type": "UMLDependencyView", "_id": dpv_id,
        "_parent": {"$ref": dgm_id}, "model": {"$ref": model_id},
        "subViews": elv,
        "lineColor": "#333333", "font": "Arial;13;0", "parentStyle": False,
        "head": {"$ref": head_view}, "tail": {"$ref": tail_view},
        "lineStyle": 1, "points": points, "showVisibility": True,
        "nameLabel": {"$ref": elv[0]["_id"]},
        "stereotypeLabel": {"$ref": elv[1]["_id"]},
        "propertyLabel": {"$ref": elv[2]["_id"]},
    }

def build_model(model_name, diagram_name, steps, pfx):
    """steps: list of box label strings, laid out vertically."""
    model_id = f"{pfx}_mdl"
    dgm_id = f"{pfx}_dgm"
    W, H, gap = 300, 50, 100
    Lx = 320
    boxes, classes, views, deps, dep_views = [], [], [], [], []
    box_ids, model_ids, tops = [], [], []
    for i, text in enumerate(steps):
        T = 80 + i * gap
        cls_id = f"{pfx}_cls{i:02d}"
        box_id = f"{pfx}_clv{i:02d}"
        classes.append({"_type": "UMLClass", "_id": cls_id,
                        "_parent": {"$ref": model_id}, "name": text})
        views.append(class_view(box_id, cls_id, f"{pfx}{i:02d}", dgm_id, Lx, T, W, H, text))
        box_ids.append(box_id); model_ids.append(cls_id); tops.append(T)
    cx = Lx + W // 2
    for i in range(len(steps) - 1):
        dep_id = f"{pfx}_dep{i:02d}"
        dpv_id = f"{pfx}_dpv{i:02d}"
        deps.append({"_type": "UMLDependency", "_id": dep_id,
                     "_parent": {"$ref": model_id},
                     "source": {"$ref": model_ids[i]},
                     "target": {"$ref": model_ids[i+1]}})
        pts = f"{cx}:{tops[i]+H};{cx}:{tops[i+1]}"
        dep_views.append(dependency_view(dpv_id, dep_id, dgm_id, box_ids[i], box_ids[i+1], pts))
    diagram = {"_type": "UMLClassDiagram", "_id": dgm_id,
               "_parent": {"$ref": model_id}, "name": diagram_name,
               "ownedViews": views + dep_views}
    return {"_type": "UMLModel", "_id": model_id,
            "_parent": {"$ref": PROJECT_ID}, "name": model_name,
            "ownedElements": [diagram] + classes + deps}

# ---- Test: uuid-generation only ----
uuid_steps = [
    "Application needs a new patient identifier",
    "Secure random source asks the OS for entropy",
    "128 random bits are produced",
    "Formatted as a 36-character string",
    "Identifier ready, no database round-trip",
]
new_model = build_model("Report - UUID generation", "uuid-generation", uuid_steps, "rptU")

with open(MDJ, "r", encoding="utf-8") as f:
    text = f.read()

anchor = '\n\t],\n\t"documentVersion"'
idx = text.rfind(anchor)
if idx == -1:
    print("ANCHOR NOT FOUND", file=sys.stderr); sys.exit(1)

dumped = json.dumps(new_model, indent="\t", ensure_ascii=False)
block = "\n".join("\t\t" + ln for ln in dumped.split("\n"))
new_text = text[:idx] + ",\n" + block + text[idx:]

# validate before writing
json.loads(new_text)

with open(MDJ, "w", encoding="utf-8") as f:
    f.write(new_text)

print("OK: inserted uuid-generation diagram; file still parses as valid JSON.")
