tool
extends EditorPlugin

const GorepWindow = preload("./GorepWindow.tscn")
const GorepPanel = preload("./GorepPanel.tscn")

var _gorep_window = null
var _gorep_panel = null

func _enter_tree():
	add_tool_menu_item("Gorep",self,"open_gorep")
	_gorep_window=GorepWindow.instance();
	get_editor_interface().get_base_control().add_child(_gorep_window)
	_gorep_panel = GorepPanel.instance()
	add_control_to_dock(DOCK_SLOT_LEFT_UR, _gorep_panel)
	
func _exit_tree():
	remove_tool_menu_item("Gorep")
	remove_control_from_docks(_gorep_panel)
	_gorep_window.queue_free()
	_gorep_panel.queue_free()
	_gorep_window = null
	_gorep_panel = null

func open_gorep(ud):
	
	print("Click to open window")
	_gorep_window.popup_centered()
	pass
