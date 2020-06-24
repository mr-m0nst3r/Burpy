#coding:utf-8
import re
import base64
import hashlib
from urllib import unquote,urlencode
from Crypto.Cipher import AES

import sys 
reload(sys)
sys.setdefaultencoding('utf-8') 

class Burpy:
    '''
    header is list, append as your need
    body is string, modify as your need
    '''
    def __init__(self):
        self.key = ""
        self.iv = ""
        self.apicode = ""
        self.head = ""

    def main(self, header, body):
        print "head:", header
        print "body:", body
        return header, body
    
    def encrypt(self, header, body):
        if(self.apicode != ''):
            print "Encryption Called"
            self.apicode = re.search(r'.*api/(\d+)\.app', header[0]).group(1)
            self.head = body.split("&")[0][len('head='):]

            data = unquote(body.split("&")[1][len('body='):])
            
            keyiv = hashlib.md5(self.apicode + unquote(self.head)).hexdigest()
            self.iv = keyiv[:16]
            self.key = keyiv[16:]

            cipher = AES.new(self.key, AES.MODE_CBC, self.iv)
            data = self.pkcs7padding(data)
            encrypted = cipher.encrypt(data)
            encrypted = base64.b64encode(encrypted)
            body_param = urlencode({"body":encrypted})

            ret_body = "head=" + self.head + "&" + body_param
            body = ret_body

        
        
        return header, body

    def decrypt(self, header, body):

        if(self.apicode != ''):
            print "Decryption Called"
            self.apicode = re.search(r'.*api/(\d+)\.app', header[0]).group(1)
            self.head = body.split("&")[0][len('head='):]

            data = unquote(body.split("&")[1][len('body='):])
            data = base64.b64decode(data, '-_')
            
            keyiv = hashlib.md5(self.apicode + unquote(self.head)).hexdigest()
            self.iv = keyiv[:16]
            self.key = keyiv[16:]

            cipher = AES.new(self.key, AES.MODE_CBC, self.iv)
            decrypted = cipher.decrypt(data)
            decrypted = self.pkcs7unpadding(decrypted)

            ret_body = "head=" + self.head + "&body=" + decrypted
            body = ret_body
        else:
            data = base64.b64decode(body)
            cipher = AES.new(self.key, AES.MODE_CBC, self.iv)
            decrypted = cipher.decrypt(data)
            body = self.pkcs7unpadding(decrypted)
        
        return header, body

    def sign(self, header, body):
        return header, body

    def processor(self, payload):
        return payload+"burpyed"

    def pkcs7padding(self, data):
        bs = AES.block_size
        padding = bs - len(data) % bs
        padding_text = chr(padding) * padding
        return data + padding_text

    def pkcs7unpadding(self, data):
        lengt = len(data)
        unpadding = ord(data[lengt - 1])
        return data[0:lengt - unpadding]