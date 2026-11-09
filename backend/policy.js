export class PolicyError extends Error {
  constructor(code, message) { super(message); this.code = code; }
}
export function validateRequest(data) {
  if (!data || typeof data.contents !== 'string' || typeof data.context !== 'string' ||
      data.contents.length > 24000 || data.context.length > 64000) {
    throw new PolicyError('invalid-argument', 'İstek boyutu veya biçimi uygun değil.');
  }
  let contents;
  try { contents = JSON.parse(data.contents); } catch { throw new PolicyError('invalid-argument', 'Mesaj biçimi geçersiz.'); }
  if (!Array.isArray(contents) || contents.length < 1 || contents.length > 12 || contents.some(m =>
    !m || !['user', 'model'].includes(m.role) || !Array.isArray(m.parts) || m.parts.length !== 1 ||
    typeof m.parts[0]?.text !== 'string' || m.parts[0].text.length > 5000)) {
    throw new PolicyError('invalid-argument', 'Mesaj biçimi geçersiz.');
  }
  return contents;
}
export function consumeQuota(previous, now) {
  const minute = Math.floor(now / 60000), day = Math.floor(now / 86400000);
  const perMinute = previous?.minute === minute ? previous.perMinute : 0;
  const perDay = previous?.day === day ? previous.perDay : 0;
  if (perMinute >= 5 || perDay >= 100) throw new PolicyError('resource-exhausted', 'AI kullanım sınırına ulaşıldı.');
  return { minute, day, perMinute: perMinute + 1, perDay: perDay + 1 };
}
export function upstreamError(status) {
  return status === 429 ? 'resource-exhausted' : [400, 404].includes(status) ? 'failed-precondition' : 'unavailable';
}
