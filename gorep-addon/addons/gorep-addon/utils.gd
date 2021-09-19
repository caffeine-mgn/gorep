extends Node

static func dependency_str_to_int(text:String) -> int:
	if (text == "EXTERNAL"):
		return 0
	if (text == "INTERNAL"):
		return 1
	return -1
	
static func dependency_int_to_str(text:int) -> String:
	if (text == 0):
		return "EXTERNAL"
	if (text == 1):
		return "INTERNAL"
	return ""

# Declare member variables here. Examples:
# var a = 2
# var b = "text"


# Called when the node enters the scene tree for the first time.
func _ready():
	pass # Replace with function body.


# Called every frame. 'delta' is the elapsed time since the previous frame.
#func _process(delta):
#	pass
