import base64
import json
import os
from pathlib import Path
import tkinter as tk
from tkinter import filedialog, messagebox
from tkinter import ttk
from Crypto.Cipher import AES
from Crypto.Util.Padding import unpad

KEY = b"123" + b"\x00" * 13

def decrypt_base64_line(s: str) -> str:
    b = base64.b64decode(s)
    c = AES.new(KEY, AES.MODE_ECB)
    p = unpad(c.decrypt(b), 16)
    try:
        return p.decode("utf-8")
    except UnicodeDecodeError:
        return p.decode("utf-8", errors="replace")

def process_file(input_path: Path, output_dir: Path) -> Path:
    output_path = output_dir / f"{input_path.stem}_decrypted.txt"
    lines_out = []
    with input_path.open("r", encoding="utf-8") as f:
        for raw in f:
            s = raw.strip()
            if not s:
                continue
            try:
                text = decrypt_base64_line(s)
                try:
                    obj = json.loads(text)
                    text = json.dumps(obj, ensure_ascii=False, indent=2)
                except json.JSONDecodeError:
                    pass
                lines_out.append(text)
            except Exception as e:
                lines_out.append(f"DECRYPT_ERROR: {e}")
    with output_path.open("w", encoding="utf-8") as wf:
        wf.write("\n\n".join(lines_out))
    return output_path

class App(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("日志解密工具 (AES/ECB/PKCS5Padding)")
        self.geometry("700x420")
        self.files = []
        self.output_dir = Path(os.getcwd())
        frm = ttk.Frame(self)
        frm.pack(fill=tk.BOTH, expand=True, padx=12, pady=12)
        btn_frame = ttk.Frame(frm)
        btn_frame.pack(fill=tk.X)
        self.btn_select = ttk.Button(btn_frame, text="选择日志文件", command=self.select_files)
        self.btn_select.pack(side=tk.LEFT)
        self.btn_out = ttk.Button(btn_frame, text="选择输出目录", command=self.select_output_dir)
        self.btn_out.pack(side=tk.LEFT, padx=8)
        self.lbl_out = ttk.Label(btn_frame, text=f"输出目录: {self.output_dir}")
        self.lbl_out.pack(side=tk.LEFT, padx=8)
        list_frame = ttk.LabelFrame(frm, text="已选择文件")
        list_frame.pack(fill=tk.BOTH, expand=True, pady=8)
        self.listbox = tk.Listbox(list_frame)
        self.listbox.pack(fill=tk.BOTH, expand=True, padx=8, pady=8)
        prog_frame = ttk.Frame(frm)
        prog_frame.pack(fill=tk.X)
        self.progress = ttk.Progressbar(prog_frame, mode="determinate")
        self.progress.pack(fill=tk.X, padx=8, pady=8)
        self.status = ttk.Label(frm, text="就绪")
        self.status.pack(fill=tk.X)
        self.btn_run = ttk.Button(frm, text="开始解密", command=self.run_decrypt)
        self.btn_run.pack(anchor=tk.E, pady=8)

    def select_files(self):
        paths = filedialog.askopenfilenames(title="选择日志文件", filetypes=[("日志或文本", "*.log *.txt"), ("所有文件", "*.*")])
        if not paths:
            return
        self.files = [Path(p) for p in paths]
        self.listbox.delete(0, tk.END)
        for p in self.files:
            self.listbox.insert(tk.END, str(p))
        self.progress.config(value=0, maximum=len(self.files))
        self.status.config(text=f"已选择 {len(self.files)} 个文件")

    def select_output_dir(self):
        d = filedialog.askdirectory(title="选择输出目录", initialdir=str(self.output_dir))
        if d:
            self.output_dir = Path(d)
            self.lbl_out.config(text=f"输出目录: {self.output_dir}")

    def run_decrypt(self):
        if not self.files:
            messagebox.showwarning("提示", "请先选择日志文件")
            return
        outs = []
        self.progress.config(value=0)
        for i, p in enumerate(self.files, start=1):
            try:
                outp = process_file(p, self.output_dir)
                outs.append(outp)
                self.progress.config(value=i)
                self.status.config(text=f"完成 {i}/{len(self.files)}: {outp.name}")
                self.update_idletasks()
            except Exception as e:
                messagebox.showerror("错误", f"处理文件失败: {p}\n{e}")
        if outs:
            messagebox.showinfo("完成", f"已生成 {len(outs)} 个解密文件")

def main():
    App().mainloop()

if __name__ == "__main__":
    main()
