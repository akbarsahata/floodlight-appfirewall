import httplib
import json
 
class StaticFlowPusher(object):
 
    def __init__(self, server):
        self.server = server
 
    def get(self, data):
        ret = self.rest_call({}, 'GET')
        return json.loads(ret[2])
 
    def set(self, data):
        ret = self.rest_call(data, 'POST')
        return ret[0] == 200
 
    def remove(self, objtype, data):
        ret = self.rest_call(data, 'DELETE')
        return ret[0] == 200
 
    def rest_call(self, data, action):
        path = '/wm/staticflowpusher/json'
        headers = {
            'Content-type': 'application/json',
            'Accept': 'application/json',
            }
        body = json.dumps(data)
        conn = httplib.HTTPConnection(self.server, 8080)
        conn.request(action, path, body, headers)
        response = conn.getresponse()
        ret = (response.status, response.reason, response.read())
        print ret
        conn.close()
        return ret
 
pusher = StaticFlowPusher('localhost')

flow1 = {
    'switch':"00:00:00:00:00:00:00:01",
    "name":"flow_mod_1",
    "in_port":"1",
    "cookie":"0",
    "priority":"1",
    "active":"true",
    "actions":"output=controller"
    }
flow2 = {
    'switch':"00:00:00:00:00:00:00:01",
    "name":"flow_mod_2",
    "in_port":"2",
    "cookie":"0",
    "priority":"1",
    "active":"true",
    "actions":"output=controller"
    }
flow3 = {
    'switch':"00:00:00:00:00:00:00:01",
    "name":"flow_mod_3",
    "in_port":"3",
    "cookie":"0",
    "priority":"1",
    "active":"true",
    "actions":"output=controller"
    }
flow4 = {
    'switch':"00:00:00:00:00:00:00:01",
    "name":"flow_mod_4",
    "in_port":"4",
    "cookie":"0",
    "priority":"1",
    "active":"true",
    "actions":"output=controller"
    }
flow5 = {
    'switch':"00:00:00:00:00:00:00:01",
    "name":"flow_mod_5",
    "in_port":"5",
    "cookie":"0",
    "priority":"1",
    "active":"true",
    "actions":"output=controller"
    }

pusher.set(flow1)
pusher.set(flow2)
pusher.set(flow3)
pusher.set(flow4) 
pusher.set(flow5)

