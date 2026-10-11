import json
import sys
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import HTTPError

key = next(line.split('=', 1)[1].strip() for line in Path('/etc/techmind/techmind.env').read_text().splitlines() if line.startswith('TECHMIND_API_KEY='))
base = sys.argv[1] if len(sys.argv) > 1 else 'http://127.0.0.1'
try:
    urlopen(base + '/conteudo/titulo', timeout=15)
    raise RuntimeError('Unauthenticated request unexpectedly accepted')
except HTTPError as error:
    assert error.code == 401, error.code
    print('Unauthenticated API: 401 as expected')
payload = json.dumps({'titulo': 'Teste de implantação OCI', 'texto': 'Teste da implantação TechMind na OCI: desenvolvimento de APIs REST com Java e Spring Boot, persistência no banco Oracle e classificação de conteúdo.', 'fonte': 'Teste de implantação'}).encode()
request = Request(base + '/conteudo', data=payload, headers={'Authorization': 'Bearer ' + key, 'Content-Type': 'application/json'})
with urlopen(request, timeout=120) as response:
    result = json.load(response)
    print('Classification and persistence HTTP:', response.status)
    print('Saved ID:', result['id'], 'Category:', result['categoria'])
request = Request(base + '/conteudo/titulo?titulo=Teste', headers={'Authorization': 'Bearer ' + key})
with urlopen(request, timeout=30) as response:
    result = json.load(response)
    print('Listing HTTP:', response.status, 'Fields:', ', '.join(result.keys()))
