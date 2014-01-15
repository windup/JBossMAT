import re

connect('${username}','${password}','${host}:${port}')

def writeMbean(f, lev):
	lev = lev + 1
	if(lev > 3):
		return
	attrs = ls(pwd(), 'true', 'a')
	if(hasattr(attrs, 'keySet') and len(attrs) > 0):
		f.write("""\t<mbean>\n""")
		f.write(''.join(["""\t\t<pwd>""", pwd(), '</pwd>', """\n"""]))
		f.write(''.join(["""\t\t<depth>""", str(lev), '</depth>', """\n"""]))
		for attr in attrs.keySet():
			f.write(''.join(["""\t\t<""", attr, '>', attrs[attr], '</', attr, '>', """\n"""]))
		f.write("""\t</mbean>\n""")
	children = ls(pwd(), 'true', 'c')
	if(hasattr(children, 'iterator')):
		for child in children:
			cd(child)
			writeMbean(f, lev)
			cd('..')
	return lev
	
f = open('${allpyout}', 'w')
f.write("""<?xml version="1.0" encoding="iso-8859-1"?>\n""")
f.write("""<mbeans>\n""")

writeMbean(f, 0)

f.write('</mbeans>')
f.close()
exit()