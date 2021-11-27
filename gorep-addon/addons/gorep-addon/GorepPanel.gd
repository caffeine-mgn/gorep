tool
extends VBoxContainer


const GorepWindow = preload("./GorepWindow.tscn")
var _gorep_window = null

func _ready():
	_gorep_window = GorepWindow.instance()
	get_parent().add_child(_gorep_window)

func _exit_tree():
	_gorep_window.queue_free()
	_gorep_window = null

func _on_Publish_pressed():
	var output=[]
	OS.execute("gorep",["publish"], true, output, true)
	for it in output:
		print(it)


func _on_Check_pressed():
	var output=[]
	OS.execute("gorep", ["check"], true, output, true)
	for it in output:
		print(it)


func _on_Dependencies_pressed():
	print("_on_Dependencies_pressed")
	_gorep_window.popup_centered()


func _on_Dependencies_button_down():
	print("1312312")


func _on_Update_pressed():
	var output=[]
	OS.execute("gorep", ["check_update"], true, output, true)
	for it in output:
		print(it)
