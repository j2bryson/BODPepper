import tkinter as tk

root = tk.Tk()

# Create canvas
canvas1 = tk.Canvas(root, width=800, height=500, relief='raised')
canvas1.pack()

# Display payoff table
payoff_table = tk.Label(root, text='Number thrown:\t1\t2\t3\t4\t5\t6\nResulting payoff:\t£1\t£2\t£3\t£4\t£5\t£0')
payoff_table.config(font=('helvetica', 12), justify='right')
canvas1.create_window(400, 75, window=payoff_table)

# Define payoff rates
payoff = [1, 2, 3, 4, 5, 0]

# Participant ID
partID = tk.Label(root, text='Participant ID:')
partID.config(font=('helvetica', 12), justify='right')
canvas1.create_window(200, 125, window=partID)

partID_entry = tk.Entry(root)
canvas1.create_window(350, 125, window=partID_entry)

instruc = tk.Label(root, text='Please enter the number, i.e. the first number you have thrown:')
instruc.config(font=('helvetica', 14))
canvas1.create_window(400, 200, window=instruc)

# Number thrown input
num_throw = tk.Label(root, text='Number thrown:')
num_throw.config(font=('helvetica', 12), justify='right')
canvas1.create_window(350, 250, window=num_throw)

num_throw_entry = tk.Entry(root)
canvas1.create_window(500, 250, window=num_throw_entry)


# on button click, show thank you display and display payoff rate
def getpayoff():
    x1 = num_throw_entry.get()
    x1_num = int(x1)

    fileid = partID_entry.get()
    with open('honesty_' + fileid + '.txt', 'w') as file_object:
        file_object.write(x1)

    canvas1.delete("all")
    finishdisp = tk.Label(root, text='Thank you!\nYour payoff is £' + str(payoff[x1_num - 1]) + '.',
                          font=('helvetica', 14, 'bold'))
    canvas1.create_window(400, 200, window=finishdisp)


# Create button
button1 = tk.Button(text='Submit', command=getpayoff, bg='green', fg='white',
                    font=('helvetica', 14, 'bold'))
canvas1.create_window(400, 350, window=button1)

root.mainloop()
