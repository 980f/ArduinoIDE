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
# created singleton ref for Base rther than adding parameters to menu builder so that it could create lambdas that need it.





