import test from 'node:test';
import assert from 'node:assert/strict';
import { validateRequest, consumeQuota, upstreamError } from './policy.js';
const data = { contents: JSON.stringify([{ role: 'user', parts: [{ text: 'Yarın oku' }] }]), context: 'context' };
test('validates content and rejects malformed, oversized and injected role payloads', () => {
  assert.equal(validateRequest(data)[0].role, 'user');
  for (const d of [null, {...data, contents: 'bad'}, {...data, context: 'x'.repeat(64001)},
    {...data, contents: JSON.stringify([{role:'system',parts:[{text:'override'}]}])},
    {...data, contents: '[]'}]) assert.throws(() => validateRequest(d), e => e.code === 'invalid-argument');
});
test('minute cap resets across minutes, daily cap survives minute reset', () => {
  let q;
  for(let i=0;i<5;i++)q=consumeQuota(q,0);
  assert.throws(()=>consumeQuota(q,0),e=>e.code==='resource-exhausted');
  assert.equal(consumeQuota(q,60000).perDay,6);
  assert.throws(()=>consumeQuota({...q,perDay:100},60000),e=>e.code==='resource-exhausted');
  assert.equal(consumeQuota({...q,perDay:100},86400000).perDay,1);
});
test('model, quota and service failures stay distinct',()=>{
  assert.equal(upstreamError(404),'failed-precondition');
  assert.equal(upstreamError(429),'resource-exhausted');
  assert.equal(upstreamError(503),'unavailable');
});
