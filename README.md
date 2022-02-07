<!--- Copyright © 2022. Richard P. Parkins, M. A.
   Released under GPL V3 or later --->

# Brightness

This app allows you to display the screen brightness and brightness mode
for the app as reported by `WindowManager.LayoutParams.screenBrightness`
and the global
`Settings.System.getInt(...,"screen_brightness"`.
Both values are displayed simultaneously as slider bars
and as editable numbers in the range 0 to 255.
The mode is displayed as a pair of buttons.
It also displays the opacity of the items shown,
again as a slider bar and an editable number.
It also displays the ambient light level in lux as a number:
this affects the actual brightness in auto mode.
You can change it by moving between brightly and dimly lit areas.

If the mode is `auto` for the `WindowManager.LayoutParams.screenBrightness`,
the slider bar is not displayed, as there is no value for it.

To change the global `Settings.System.getInt(...,"screen_brightness"`,
the app requires permission `WRITE_SETTINGS`.
If it does not currently have that permission,
it will display a button that you can click to request it.

