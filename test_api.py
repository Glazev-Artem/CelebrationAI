import httpx
try:
    r = httpx.get('https://45.80.131.13.nip.io/docs', verify=False, timeout=5)
    print('Status:', r.status_code)
except Exception as e:
    print('Error:', str(e))
