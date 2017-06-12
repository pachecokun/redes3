import smtplib
import mimetypes
import email
import email.mime.application

def sendEmail(recvr):
	FILENAME = 'Template.pdf'
	MAIL_SENDR = 'demisgmoncada@gmail.com'
	MAIL_PSW = 'MychO268'
	
	msg = email.mime.Multipart.MIMEMultipart()
	msg['Subject'] = 'Reporte'
	msg['From'] = 'Servidor <'+MAIL_SENDR+'>'
	msg['To'] = 'Usuario <'+recvr+'>'

	body = email.mime.Text.MIMEText("""Plantilla de configuracion basica de red.""")
	msg.attach(body)

	# PDF attachment
	fp=open(FILENAME,'rb')
	att = email.mime.application.MIMEApplication(fp.read(),_subtype="pdf")
	fp.close()
	att.add_header('Content-Disposition','attachment',filename=FILENAME)
	msg.attach(att)

	try:
		smtpObj = smtplib.SMTP("smtp.gmail.com", 587)
		smtpObj.ehlo()
		smtpObj.starttls()
		smtpObj.login(MAIL_SENDR, MAIL_PSW)
		smtpObj.sendmail(MAIL_SENDR, [recvr], msg.as_string())
		smtpObj.close()    
		print( "email enviado")
	except Exception as e:
		print("Error: no fue posible enviar email")
		print(e)
	return

mail = raw_input("direccion de email donde se enviara el manual: ")
sendEmail(mail)

