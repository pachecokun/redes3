import PIL
from PIL import Image

from reportlab.lib.enums import TA_JUSTIFY
from reportlab.lib.pagesizes import A4
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Image
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle

import smtplib
import mimetypes
import email
import email.mime.application

from os import listdir
from os.path import isfile, join

OUT_FILENAME = "report.pdf"
MAIL_SENDER = "demisgmoncada@gmail.com"
MAIL_RECEIVER = ["demisgm@live.com", "mikorreomola@gmail.com"]
MAIL_PASSWORD = "MychO268"
FIG_PATH = "figures/"

def sendEmail():
	msg = email.mime.Multipart.MIMEMultipart()
	msg['Subject'] = 'Reporte de Rendimiento'
	msg['From'] = 'Servidor <' + MAIL_SENDER + '>'
	msg['To'] = 'Usuario <'+ MAIL_RECEIVER[0] +'>'

	body = email.mime.Text.MIMEText("""Reporte de rendimiento de red.""")
	msg.attach(body)

	# Adjuntar pdf
	fp=open(OUT_FILENAME,'rb')
	att = email.mime.application.MIMEApplication(fp.read(),_subtype="pdf")
	fp.close()
	att.add_header('Content-Disposition','attachment',filename=OUT_FILENAME)
	msg.attach(att)

	try:
		smtpObj = smtplib.SMTP("smtp.gmail.com", 587)
		smtpObj.ehlo()
		smtpObj.starttls()
		smtpObj.login(MAIL_SENDER, "MychO268")
		smtpObj.sendmail(MAIL_SENDER, MAIL_RECEIVER, msg.as_string())
		smtpObj.close()    
		print( "e-mail enviado.")
	except Exception as e:
		print(e)
		print("Error: no se pudo enviar e-mail")
	return

def getSize(path):
	WIDTH = 300
	img = PIL.Image.open(path)
	percent = WIDTH / float(img.size[0])
	height = int(float(img.size[1]) * percent)
	return (WIDTH, height)
	
def isImage(path):
	try:
		img = PIL.Image.open(path)
		return 1
	except IOError as e:
		return 0
    	

# Inicializar documento
doc = SimpleDocTemplate(OUT_FILENAME,pagesize=A4,
                        rightMargin=72,leftMargin=72,
                        topMargin=72,bottomMargin=18)
Story=[]

styles = getSampleStyleSheet()
text = "<font size = 16>Performance metrics Report</font>"
Story.append(Paragraph(text, styles["Normal"]))
Story.append(Spacer(1, 12))

# Obtener las graficas
graphs = [f for f in listdir(FIG_PATH) if isfile(join(FIG_PATH, f))]
for g in graphs:
	path = FIG_PATH + g
	if isImage(path):
		size = getSize(path)
		im = Image(path, size[0], size[1])
		Story.append(im)

doc.build(Story)

sendEmail()
