import httpx
try:
    r = httpx.get('http://45.80.131.13:8000/docs', timeout=5)
    print('Status HTTP:', r.status_code)
except Exception as e:
    print('Error HTTP:', str(e))
