from Tkinter import *
from snmp import *
import time

class GUI():

	nodes = {}
	data = None
	selected_node = None
	selected_interface = None
	
	def select_node(self,e):
		time.sleep(0.001)
		w = e.widget
		self.selected_node = w.get(w.curselection()[0])
		self.list_interfaces.delete(0,END)
		self.selected_interface = None
		print(self.selected_node)
		for i in self.data[self.selected_node]:
			self.list_interfaces.insert(END,i['name'])

	def select_interface(self,e):
		w = e.widget
		self.selected_interface = int(w.curselection()[0])
		self.draw()
	
	def add_node(self,ip):
		self.nodes[ip] = []
		self.list_nodes.insert(END,ip)
		
	def add_interface(self,ip,interface):
		self.nodes[ip].append(interface)
		self.list_interfaces.insert(END,interface)
		
	def update(self,data):
		self.data = data
		self.draw()
		
	def draw(self):
		c = self.plot
		c.delete(ALL)
		if self.selected_node != None and self.selected_interface != None:
			bw_r = self.data[self.selected_node][self.selected_interface]['bwr']
			bw_t = self.data[self.selected_node][self.selected_interface]['bwt']
			count = len(bw_r)
			
			self.lbl_if.configure(text=self.selected_node+' - '+self.list_interfaces.get(self.selected_interface))
			
			c.create_rectangle(0,0,200,100,fill='white')
			
			if count>0:
				max_r = max(bw_r)
				max_t = max(bw_t)
				max_value = max(max_r,max_t)
				step_x = 200/count
				self.lbl_max.configure(text=str(round(max_value/1024,2))+'kB/s')
				if max_value >0:
					step_y = (100-10)/max_value
				else:
					step_y = 1
				for i in range(count):
					if i>0:
						c.create_line(int((i-1)*step_x),100-int(bw_r[i-1]*step_y),int(i*step_x),100-int(bw_r[i]*step_y),fill = 'blue')
						c.create_line(int((i-1)*step_x),100-int(bw_t[i-1]*step_y),int(i*step_x),100-int(bw_t[i]*step_y),fill = 'red')
				
		else:
			self.lbl_if.configure(text='Seleccione una interfaz')
			self.lbl_max.configure(text='')
			
	
	def init(self):

		base = Tk()
		base.geometry("500x200+30+30") 
	
		l = Label(base,text="Dispositivos")
		l.place(x=10,y=10,width=100,height=25)
	
		self.list_nodes = Listbox(base,selectmode=SINGLE)
		self.list_nodes.place(x=10,y=30,width=100,height=150)
		self.list_nodes.bind('<<ListboxSelect>>', self.select_node)
	
		l2 = Label(base,text="Interfaces")
		l2.place(x=110,y=10,width=100,height=25)
	
		self.list_interfaces = Listbox(base,selectmode=SINGLE)
		self.list_interfaces.place(x=110,y=30,width=100,height=150)
		self.list_interfaces.bind('<<ListboxSelect>>', self.select_interface)
	
		self.plot = Canvas(base)
		self.plot.place(x=290,y=50,width=200,height=100)
		
		self.lbl_if = Label(base,text='Seleccione una interfaz')
		self.lbl_if.place(x=250,y=20,width=200,height=20)
		
		self.lbl_max = Label(base,text='',anchor='e')
		self.lbl_max.place(x=210,y=50,width=80,height=20)

		base.mainloop()
