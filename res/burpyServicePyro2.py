#coding:utf-8
import Pyro4
import sys

m_module = sys.argv[-1]

import importlib.util
module_name = m_module.split("/")[-1][:-2]
spec = importlib.util.spec_from_file_location('{}.Burpy'.format(module_name),m_module)
b = importlib.util.module_from_spec(spec)
spec.loader.exec_module(b)

from base64 import b64decode as b64d

class http:
    '''
    accept data string
    '''

    def __init__(self,data):
        self.data = data
        self.first_line, self.headers, self.body = self.parse_headers_and_body(data)

    def parse_headers(self, s):
        headers = s.split("\r\n")
        headers_found = {}
        for header_line in headers:
            try:
                key, value = header_line.split(':', 1)
            except:
                continue
            headers_found[key] = value.strip()
        return headers_found
    
    def parse_headers_and_body(self,s):
        try:
            crlfcrlf = b"\x0d\x0a\x0d\x0a"
            crlfcrlfIndex = s.find(crlfcrlf)
            headers = s[:crlfcrlfIndex + len(crlfcrlf)].decode("utf-8")
            body = s[crlfcrlfIndex + len(crlfcrlf):]
        except:
            headers = s
            body = ''
        first_line, headers = headers.split("\r\n", 1)
        return first_line.strip(), self.parse_headers(headers), str(body,encoding="utf-8")

    # def build(self, isRequest):
    def build(self):
        '''
        convert self to strings
        '''
        # if(isRequest):
            # get headers as dict
        newhttp = list()
        newhttp.append(self.first_line)
        for k in self.headers.keys():
            newhttp.append("{}: {}".format(k,self.headers[k]))

        newhttp.append("")
        newhttp.append(self.body)
        newhttp = map(lambda l: l if isinstance(l, bytes) else l.encode('utf-8'), newhttp)

        http_str = b'\r\n'.join(newhttp)
        return str(http_str,encoding="utf-8")
            
        # else:
        #     return "response"
        # return p

class Unbuffered(object):
   def __init__(self, stream):
       self.stream = stream
   def write(self, data):
       self.stream.write(data)
       self.stream.flush()
   def writelines(self, datas):
       self.stream.writelines(datas)
       self.stream.flush()
   def __getattr__(self, attr):
       return getattr(self.stream, attr)

@Pyro4.expose
class BridaServicePyro:
    def __init__(self, daemon):
        self.daemon = daemon
        self.burpy = b.Burpy()

    def hello_spawn(self):
        data = "it's working"
        return data

    def hello(self,http_b_64):
        data = http(b64d(http_b_64))
        
        if data is None:
            return "Parse HTTP data failed"
        try:
            # headers is dict, body is str
            data.headers, data.body = self.burpy.main(data.headers, data.body)
            ret_val = data.build()
        except Exception as e:
            print( e )
            ret_val = "Main in BurpyService failed"
        return ret_val

    def encrypt(self, http_b_64):
        data = http(b64d(http_b_64))
        
        if data is None:
            return "Parse HTTP data failed"
        try:
            # headers is dict, body is str
            data.headers, data.body = self.burpy.encrypt(data.headers, data.body)
            ret_val = data.build()
        except Exception as e:
            print( e )
            ret_val = "Encrypt in BurpyService failed"
        return ret_val

    def decrypt(self, http_b_64):
        data = http(b64d(http_b_64))
        
        if data is None:
            return "Parse HTTP data failed"
        try:
            # headers is dict, body is str
            data.headers, data.body = self.burpy.decrypt(data.headers, data.body)
            ret_val = data.build()
        except Exception as e:
            print( e )
            ret_val = "Decrypt in BurpyService failed"
        return ret_val

    def sign(self, http_b_64):
        data = http(b64d(http_b_64))
        
        if data is None:
            return "Parse HTTP data failed"
        try:
            # headers is dict, body is str
            data.headers, data.body = self.burpy.sign(data.headers, data.body)
            ret_val = data.build()
        except Exception as e:
            print( e )
            ret_val = "Sign in BurpyService failed"
        return ret_val

    def processor(self, data):
        try:
            ret_val = self.burpy.processor(data)
            return ret_val
        except Exception as e:
            print( e )
            return "Can't process payload"


    @Pyro4.oneway
    def shutdown(self):
        print('shutting down...')
        try:
            # self.burpy.down()
            del self.burpy
        except Exception:
            print("burpy.down() method not found, skipped.")
        self.daemon.shutdown()

# Disable python buffering (cause issues when communicating with Java...)
sys.stdout = Unbuffered(sys.stdout)
sys.stderr = Unbuffered(sys.stderr)

host = sys.argv[1]
port = int(sys.argv[2])
daemon = Pyro4.Daemon(host=host,port=port)

#daemon = Pyro4.Daemon(host='127.0.0.1',port=9999)
bs = BridaServicePyro(daemon)
uri = daemon.register(bs,objectId='BurpyServicePyro')

print("Ready.")
daemon.requestLoop()
