tool
extends WindowDialog


# Declare member variables here. Examples:
# var a = 2
# var b = "text"
const Utils = preload("./utils.gd")
const DependencyWindow = preload("./DependencyWindow.tscn")

# Called when the node enters the scene tree for the first time.
func _ready():
	win = DependencyWindow.instance()
	get_parent().add_child(win)
	win.connect("finished", self, "_dependency_window_finished")
	win.connect("cancelled", self, "_depepdency_window_cancelled")

var manifest = null
onready var addBtn:Button = $Dependencies/Buttons/AddBtn
onready var removeBtn:Button = $Dependencies/Buttons/RemoveBtn
onready var editBtn:Button = $Dependencies/Buttons/EditBtn
onready var dependencies:ItemList = $Dependencies/List

# Called every frame. 'delta' is the elapsed time since the previous frame.
#func _process(delta):
#	pass

var win=null
var editing_dependency_index = -1




func _on_RemoveBtn_pressed():
	var selected = dependencies.get_selected_items()
	if (selected.size()==1):
		manifest["dependencies"].remove(selected[0])
		dependencies.remove_item(selected[0])


func _on_GorepWindow_about_to_show():
	dependencies.clear()
	var f = File.new()
	var ok = f.open("res://gorep_project.json", File.READ)
	if (ok == 0):
		var txt=f.get_as_text()
		print("text: " + txt)
		manifest = JSON.parse(txt).result
		f.close()
		
		for it in manifest["dependencies"]:
			dependencies.add_item(it["name"]+":"+it["version"]+":"+it.get("type", "EXTERNAL"))
	else:
		pass


func _on_List_item_selected(index):
	var selected = dependencies.get_selected_items()
	editBtn.disabled = selected.size() != 1
	removeBtn.disabled = selected.size() != 1

func _on_AddBtn_pressed():
	editing_dependency_index = -1
	win.popupNew()

func _dependency_window_finished():
	var name = win.artifactName.text
	var version = win.artifactVersion.text
	var typeId = win.artifactType.selected
	
	var type = Utils.dependency_int_to_str(typeId)
	if (editing_dependency_index == -1):
		dependencies.add_item(name + ":" + version + ":" + type)
		manifest["dependencies"].append({
			"name": name,
			"version": version,
			"type": type
		})
		dependencies.select(dependencies.get_item_count()-1)
	else:
		dependencies.set_item_text(editing_dependency_index, name + ":" + version + ":" + type)
		manifest["dependencies"][editing_dependency_index]={
			"name": name,
			"version": version,
			"type": type
		}
		dependencies.select(editing_dependency_index)
	save()

func _depepdency_window_cancelled():
	editing_dependency_index = -1


func _on_EditBtn_pressed():
	var selected = dependencies.get_selected_items()
	if (selected.size() == 1):
		editing_dependency_index = selected[0]
		var e = manifest["dependencies"][selected[0]]
		var type = e.get("type","EXTERNAL")
		
		var typeId = Utils.dependency_str_to_int(type)
		win.popupEdit(e.get("name"),e.get("version"), typeId)

func save():
	var json = JSON.print(manifest)
	var f = File.new()
	var ok = f.open("res://gorep_project.json", File.WRITE)
	if (ok == 0):
		var txt=f.store_string(json)
		f.close()
		print("gorep_project.json updated")


func _on_List_gui_input(event):
	if event is InputEventMouseButton and event.button_index == BUTTON_LEFT:
		if event.doubleclick:
			_on_EditBtn_pressed()


func _on_List_nothing_selected():
	editBtn.disabled = true
	removeBtn.disabled = true
