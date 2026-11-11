"""Real Firestore rules tests; synthetic identities/data only, against a demo emulator.

Run via firebase emulators:exec --project demo-rotam-sync --config
 firebase.test.json --only firestore 'python3 tests/firestore/test_rules.py'.
No production credentials, SDK dependencies, or personal plans are used.
"""
import base64
import json
import os
import time
import unittest
import urllib.error
import urllib.request
from copy import deepcopy

PROJECT = 'demo-rotam-sync'
HOST = os.environ.get('FIRESTORE_EMULATOR_HOST', '')
assert HOST in ('127.0.0.1:8080', 'localhost:8080'), 'An explicit local demo emulator is required'
BASE = f'http://{HOST}/v1/projects/{PROJECT}/databases/(default)/documents'

def token(uid):
    def b64(obj):
        return base64.urlsafe_b64encode(json.dumps(obj).encode()).rstrip(b'=').decode()
    now = int(time.time())
    return b64({'alg': 'none', 'typ': 'JWT'}) + '.' + b64({
        'iss': f'https://securetoken.google.com/{PROJECT}', 'aud': PROJECT,
        'iat': now, 'exp': now + 3600, 'auth_time': now, 'sub': uid, 'user_id': uid,
        'firebase': {'sign_in_provider': 'custom', 'identities': {}}
    }) + '.'

def request(method, suffix, uid=None, data=None):
    headers = {'Content-Type': 'application/json'}
    if uid:
        headers['Authorization'] = 'Bearer ' + token(uid)
    req = urllib.request.Request(BASE + suffix, method=method, headers=headers,
                                 data=json.dumps(data).encode() if data is not None else None)
    try:
        with urllib.request.urlopen(req, timeout=20) as result:
            return result.status, json.load(result)
    except urllib.error.HTTPError as error:
        return error.code, json.load(error)

def fields(revision=1):
    return {'schema': {'integerValue': '1'}, 'revision': {'integerValue': str(revision)},
            'writer': {'stringValue': 'synthetic-installation'},
            'payload': {'bytesValue': base64.b64encode(b'not-personal-test-fixture').decode()}}

def write(path, data, uid, server_time=True):
    value = {'update': {'name': f'projects/{PROJECT}/databases/(default)/documents/{path}', 'fields': data}}
    if server_time:
        value['updateTransforms'] = [{'fieldPath': 'updatedAt', 'setToServerValue': 'REQUEST_TIME'}]
    return request('POST', ':commit', uid, {'writes': [value]})[0]

class SnapshotRules(unittest.TestCase):
    def setUp(self):
        self.uid = 'test-' + self._testMethodName
        self.path = f'users/{self.uid}/sync/snapshot'
        self.assertEqual(200, write(self.path, fields(), self.uid))

    def test_owner_reads_and_revises(self):
        self.assertEqual(200, request('GET', '/' + self.path, self.uid)[0])
        self.assertEqual(200, write(self.path, fields(2), self.uid))
        self.assertEqual(200, write(self.path, fields(3), self.uid))

    def test_unauthenticated_and_other_account_cannot_access(self):
        for uid in (None, 'other-user'):
            with self.subTest(uid=uid):
                self.assertEqual(403, request('GET', '/' + self.path, uid)[0])
                self.assertEqual(403, write(self.path, fields(2), uid))
                self.assertEqual(403, request('DELETE', '/' + self.path, uid)[0])
                self.assertEqual(403, write(f'users/{self.uid}/sync/another', fields(), uid))

    def test_cannot_create_someone_elses_snapshot(self):
        self.assertEqual(403, write('users/someone-else/sync/snapshot', fields(), self.uid))

    def test_invalid_initial_revision(self):
        for revision in (0, -1, 2, 9007199254740992):
            uid = self.uid + '-' + str(revision)
            self.assertEqual(403, write(f'users/{uid}/sync/snapshot', fields(revision), uid))

    def test_stale_or_skipped_revision(self):
        for revision in (0, 1, 3, 9007199254740992):
            self.assertEqual(403, write(self.path, fields(revision), self.uid))

    def test_schema_types_fields_and_size_limits_on_create_and_update(self):
        variants = []
        def change(key, value):
            obj = fields(2)
            if value is None:
                obj.pop(key)
            else:
                obj[key] = value
            variants.append((key + str(len(variants)), obj))
        change('schema', {'integerValue': '2'})
        change('schema', {'stringValue': '1'})
        change('revision', {'stringValue': '2'})
        change('writer', {'integerValue': '1'})
        change('writer', {'stringValue': ''})
        change('writer', {'stringValue': 'w' * 101})
        change('payload', {'stringValue': 'wrong-type'})
        change('payload', {'bytesValue': ''})
        change('payload', {'bytesValue': base64.b64encode(b'x' * 716801).decode()})
        for key in ('schema', 'writer', 'payload'):
            change(key, None)
        for key in ('ownerUid', 'role', 'isAdmin', 'unexpected'):
            change(key, {'stringValue': 'admin'})
        for number, (label, obj) in enumerate(variants):
            with self.subTest(label=label, operation='update'):
                self.assertEqual(403, write(self.path, obj, self.uid))
            uid = self.uid + str(number)
            fresh = deepcopy(obj)
            if fresh.get('revision') == {'integerValue': '2'}:
                fresh['revision'] = {'integerValue': '1'}
            with self.subTest(label=label, operation='create'):
                self.assertEqual(403, write(f'users/{uid}/sync/snapshot', fresh, uid))

    def test_timestamp_must_be_server_time(self):
        self.assertEqual(403, write(self.path, fields(2), self.uid, server_time=False))
        for value in ({'stringValue': 'now'}, {'timestampValue': '2020-01-01T00:00:00Z'},
                      {'timestampValue': '2099-01-01T00:00:00Z'}):
            obj = fields(2)
            obj['updatedAt'] = value
            self.assertEqual(403, write(self.path, obj, self.uid, server_time=False))

    def test_delete_and_unrelated_paths_stay_denied(self):
        self.assertEqual(403, request('DELETE', '/' + self.path, self.uid)[0])
        for path in (f'users/{self.uid}', f'users/{self.uid}/sync/other',
                     f'users/{self.uid}/private/details', 'serverQuotas/example'):
            self.assertEqual(403, write(path, fields(), self.uid))
            self.assertEqual(403, request('GET', '/' + path, self.uid)[0])
        self.assertEqual(403, request('GET', '/users', self.uid)[0])
        self.assertEqual(403, request('GET', '/users')[0])

if __name__ == '__main__':
    unittest.main(verbosity=2)
