tool
extends EditorPlugin

const GorepWindow = preload("./GorepWindow.tscn")

var _gorep_window = null

func _enter_tree():
	add_tool_menu_item("Gorep",self,"open_gorep")
	_gorep_window=GorepWindow.instance();
	get_editor_interface().get_base_control().add_child(_gorep_window)
	
func _exit_tree():
	remove_tool_menu_item("Gorep")
	_gorep_window.queue_free()
	_gorep_window = null

func open_gorep(ud):
	
	print("Click to open window")
	_gorep_window.popup_centered()
	pass
