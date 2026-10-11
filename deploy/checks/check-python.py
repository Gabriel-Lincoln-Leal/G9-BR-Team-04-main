import json
from pathlib import Path
from urllib.request import Request, urlopen

key = next(line.split('=', 1)[1].strip() for line in Path('/etc/techmind/techmind.env').read_text().splitlines() if line.startswith('TECHMIND_API_KEY='))
payload = json.dumps({'titulo': 'Introdução ao Java e Spring Boot', 'texto': 'Desenvolvimento de APIs REST com Java, Spring Boot e banco de dados Oracle.'}).encode()
request = Request('http://127.0.0.1:8000/predizer', data=payload, headers={'Authorization': 'Bearer ' + key, 'Content-Type': 'application/json'})
with urlopen(request, timeout=60) as response:
    result = json.load(response)
    print('Classification HTTP:', response.status)
    print('Result fields:', ', '.join(result.keys()))
