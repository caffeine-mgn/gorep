tool
extends WindowDialog

# Declare member variables here. Examples:
# var a = 2
# var b = "text"
onready var okBtn = $Buttons/OkBtn
onready var cancelBtn = $Buttons/CancelBtn
onready var artifactName:TextEdit = $ArtifactName
onready var artifactVersion:TextEdit = $ArtifactVersion
onready var artifactType:OptionButton = $ArtifactType
signal finished
signal cancelled

# Called when the node enters the scene tree for the first time.
func _ready():
	get_close_button().connect("pressed", self, "_on_CancelBtn_pressed")

func popupEdit(name:String, version:String, typeId:int):
	assert(typeId >= 0 && typeId <= 1, "typeId shoud be 0 or 1")
	self.window_title = "Edit Dependency"
	artifactName.text = name
	artifactVersion.text = version
	artifactType.selected = typeId
	_check_valid()
	popup_centered()

func popupNew():
	self.window_title = "New Dependency"
	artifactName.text = ""
	artifactVersion.text = "1.0"
	artifactType.selected = 0
	_check_valid()
	popup_centered()
#	hide()
#	if (get_parent()!=null):
#		get_parent().remove_child(self)
#	self.queue_free()


func _on_OkBtn_pressed():
	emit_signal("finished")
	hide()

func _on_CancelBtn_pressed():
	emit_signal("cancelled")
	hide()


func _check_valid():
	var valid = artifactName.text.length()>0 && artifactVersion.text.length()>0
	okBtn.disabled = !valid
