"""Wait only for the synthetic local emulators, never the production project."""
import socket
import time
end=time.monotonic()+90
while time.monotonic()<end:
    try:
        for port in (8080,9099):
            with socket.create_connection(('127.0.0.1',port),timeout=1):
                pass
        print('Synthetic Firestore and Auth emulators are listening')
        break
    except OSError:
        time.sleep(1)
else:
    raise SystemExit('Demo emulators did not start; inspect firebase-demo.log')
