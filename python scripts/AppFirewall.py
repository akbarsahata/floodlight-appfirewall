#!/usr/bin/python

import sys, getopt
import httplib
import json

class AppFirewall(object):
 
    def __init__(self, server):
        self.server = server
 
    def get(self, data):
        path = '/wm/appfirewall/list'
        ret = self.rest_call(data, path, 'GET')
        return json.loads(ret[2])
 
    def add(self, data):
        path = '/wm/appfirewall/add'
        ret = self.rest_call(data, path, 'GET')
        return ret[2]
 
    def remove(self, data):
        path = '/wm/appfirewall/delete'
        ret = self.rest_call(data, path, 'DELETE')
        return ret[2]
 
    def rest_call(self, data, path, action):
        headers = {
            'Content-type': 'text/plain',
            'Accept': 'text/plain, application/json',
            }
        conn = httplib.HTTPConnection(self.server, 8080)
        conn.request(action, path, data, headers)
        response = conn.getresponse()
        ret = (response.status, response.reason, response.read())
        print ret
        conn.close()
        return ret

pusher = AppFirewall('localhost')

def main(argv):
    try:
        opts, args = getopt.getopt(argv, 'a:d:h', ['add=', 'delete=', 'help'])
    except getopt.GetoptError:
        print 'AppFirewall.py [-a/-d/-l] <URL>'
        sys.exit(2)
    for opt, arg in opts:
        if opt in ('-a', '--add'):
            print 'adding URL'
            print (arg)
            if arg == '':
                print 'URL is empty'
            else:
                pusher.add(arg)
        elif opt in ('-d', '--delete'):
            print 'deleting URL'
            print (arg)
            if arg == '':
                print 'URL is empty'
            else:
                pusher.remove(arg)
        elif opt in ('-h', '--help'):
             print 'AppFirewall.py [-a/-d/-l] <URL>\n'
             print 'options:'
             print '-a <URL> / --add <URL> (to add URL, example: HTTP://<domain>/subdir1/subdir2/.../subdirN)'
             print '-d <URL> / --delete <URL> (to delete URL, CAUTION: ending with * will delete recursively)'
             print '-h (for help)'
    sys.exit(2)

if __name__ == "__main__":
    main(sys.argv[1:])






 

