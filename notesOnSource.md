lint issues:

# use of array of int instead of a struct for window settings preferences, Editor::getPlacement(). have parse and pack members of an interface to isolate details from Base.
# unnecessary (redundant) type argument in constructor of generic. eg: Base::editors (seems to have been fixed by later pull)
# synching on non-final object (Base::editors).
# use of C style array declarations
# StringBuffer used when StringBuilder is generally better.
# intentionally empty catch blocks should be annotated, usual is to name exception 'ignored'
# use Path class instead of inline this+FileSeparator+that+FileSeparator


repo issues:

# liblistSerialsj.so was not present, copied in from an installation.
# looks for preferences.txt in the CWD launched in, expected HOME directory, use APP_DIR. If APP_DIR not set should do something other than CWD which is not nearly as easy to set for GUI launchers as one might like, at least on gnome 3.x.

documentation:
# check that examples/01.basics/bareminimum... is documented as being the template for new sketches.

bad things I did:
# created singleton ref for Base rather than adding parameters to menu builder so that it could create lambdas that need it.


commandline testing issues:
# preferences.txt not found even though it was in home/.arduino15, linked to it from app/lib
# lib/themes directory not present, is there a note somewhere on how to build the delivery environment?
# lib/public.gpg.key ditto
# "Illegal reflective access by processing.app.linux.GTKLookAndFeelFixer (file:/d/bin/ArduinoIDE/app/lib/app980f.jar) to field com.sun.java.swing.plaf.gtk.GTKLookAndFeel.styleFactory"
# java.lang.NullPointerException \n\t at cc.arduino.contributions.packages.ContributionsIndexer.syncBuiltInHardware(ContributionsIndexer.java:221)
# couldn't just drop the jar into 1.8.9, too many new libs.






