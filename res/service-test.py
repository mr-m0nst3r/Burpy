# coding:utf-8

import Pyro5.api

uri = "PYRO:BurpyServicePyro@127.0.0.1:10999"
g = Pyro5.api.Proxy(uri)

print(g.get_methods())