#coding:utf-8
import codecs
import Pyro4
import sys
m_module = sys.argv[-1]
import imp
try:
    imp.load_source("f",m_module) #imp.load_source("test","/tmp/test.py")
except Exception:
    print("Failed to get python file, pls recheck")
from f import Burpy # which loads the destination file


@Pyro4.expose
class BridaServicePyro:
    def __init__(self, daemon):
        self.daemon = daemon
        self.burpy = Burpy()

    def disconnect_application(self):

        self.device.kill(self.pid)
        return

    def hello_spawn(self):
        data = "it's working"
        return data

    def hello(self, header, body):
        data = body[0].decode("hex")
        if data is None:
        	return "No data selected"
        try:
            # header is a list, but body is string
            # so we append body to header list
            nheader, nbody = self.burpy.main(header, data)
            nheader.append("")
            nheader.append(nbody)
            http_str = "\x0d\x0a".join(nheader)
            ret_val = http_str

        except Exception as e:
            print( e )
            ret_val = "helo(main) BurpyService failed"
        return ret_val

    def encrypt(self, header, body):
        data = body[0].decode("hex")
        if data is None:
            return "No data selected"
        try:
            # header is a list, but body is string
            # so we append body to header list
            nheader, nbody = self.burpy.encrypt(header, data)
            header.append("")
            nheader.append(nbody)
            http_str = "\x0d\x0a".join(nheader)
            ret_val = http_str

        except Exception as e:
            print( e )
            ret_val = "Encrypt in BurpyService failed"
        return ret_val

    def decrypt(self, header, body):
        data = body[0].decode("hex")
        if data is None:
            return "No data selected"
        try:
            # header is a list, but body is string
            # so we append body to header list
            nheader, nbody = self.burpy.decrypt(header, data)
            nheader = nheader or list()
            nheader.append("")
            nheader.append(nbody)
            http_str = "\x0d\x0a".join(nheader)
            ret_val = http_str

        except Exception as e:
            print( e )
            ret_val = "Decrypt in BurpyService Failed"
        return ret_val

    def sign(self, header, body):
        data = body[0].decode("hex")
        if data is None:
            return "No data selected"
        try:
            # header is a list, but body is string
            # so we append body to header list
            nheader, nbody = self.burpy.sign(header, data)
            nheader.append("")
            nheader.append(nbody)
            http_str = "\x0d\x0a".join(nheader)
            ret_val = http_str

        except Exception as e:
            print( e )
            ret_val = "Can't find method name burpy or script file not found"
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


host = sys.argv[1]
port = int(sys.argv[2])
daemon = Pyro4.Daemon(host=host,port=port)

#daemon = Pyro4.Daemon(host='127.0.0.1',port=9999)
bs = BridaServicePyro(daemon)
uri = daemon.register(bs,objectId='BurpyServicePyro')

print("Ready.")
daemon.requestLoop()
