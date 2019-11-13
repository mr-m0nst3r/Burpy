import codecs
import Pyro4
import sys
m_module = sys.argv[-1]
import imp
imp.load_source("f",m_module) #imp.load_source("test","/tmp/test.py")
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
        m_param = 'spawn test'
        return m_param

    def hello(self,data):
        data = data.decode("hex")
        if data is None:
        	return "No data selected"
        try:
            ret_val = self.burpy.main(data)
        except Exception as e:
            print e
            ret_val = "Can't find method name burpy or script file not found"
        return ret_val

    @Pyro4.oneway
    def shutdown(self):
        print('shutting down...')
        self.burpy.down()
        self.daemon.shutdown()


host = sys.argv[1]
port = int(sys.argv[2])
daemon = Pyro4.Daemon(host=host,port=port)

#daemon = Pyro4.Daemon(host='127.0.0.1',port=9999)
bs = BridaServicePyro(daemon)
uri = daemon.register(bs,objectId='BurpyServicePyro')

print("Ready.")
daemon.requestLoop()
