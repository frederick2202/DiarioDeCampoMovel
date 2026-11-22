# Diário de Campo Móvel — Módulo 09 (Recursos do Dispositivo & APIs Web)

**Instituição:** Universidade de Uberaba (Uniube)  
**Curso:** Bacharelado em Sistemas de Informação  
**Disciplina:** Desenvolvimento para Dispositivos Móveis — Módulo 09  
**Professor:** Prof. Igor Junqueira  
**Aluno:** Frederico Arantes Lima Ferreira  

---

## 1. Visão Geral do Projeto

O **Diário de Campo Móvel** é uma aplicação Android nativa desenvolvida em **Java** que orquestra recursos de hardware do dispositivo (Câmera, GPS e Sensor de Luminosidade Ambiente) e integrações com APIs REST externas (**ViaCEP** e **OpenWeather**) sob uma arquitetura reativa baseada em **MVVM (`ViewModel` + `LiveData` + `Repository` + `ExecutorService`)**.

### Requisitos Atendidos
1. **Tela de Registro Completa em Java:** Coleta título textual, CEP opcional, foto capturada da câmera (`TakePicturePreview`), data/hora automática, coordenadas GPS (`FusedLocationProviderClient`) e leitura de luminosidade ambiente (`Sensor.TYPE_LIGHT`).
2. **Persistência de Sessão Múltipla:** Estrutura no `DiarioRepository` + `DiarioViewModel` que preserva 3 ou mais registros consecutivos na sessão, sobrevivendo à rotação de tela sem perda de estado.
3. **Enriquecimento via API Externa:** Ao tocar em qualquer registro da lista (`RecyclerView`), o aplicativo executa uma chamada HTTP assíncrona via **Retrofit 2 + Gson** para enriquecer os dados locais com informações de logradouro/município (ViaCEP) ou condições climáticas das coordenadas (OpenWeather).
4. **Máquina de Estados Explícita na UI (`UiState`):**
   - `UiState.Idle`: Tela inicial pronta para interação.
   - `UiState.Loading`: Exibe `ProgressBar` e mensagem de carregamento sem congelar a *Main Thread*.
   - `UiState.Success`: Atualiza a lista de registros e apresenta os dados enriquecidos pela API.
   - `UiState.Error`: Informa falhas de rede, timeout ou erro lógico (`{"erro": true}` no ViaCEP) orientando o próximo passo do usuário.
   - `UiState.PermissionDenied`: Trata a recusa de permissões em tempo de execução com degradação graciosa e botão direto para abrir as Configurações do aplicativo.
5. **Desafio Opcional Incorporado:** Monitoramento de luminosidade ambiente com `SensorManager`, filtro passa-baixa para atenuação de ruído numérico e remoção automática do listener no ciclo `onPause()`.

---

## 2. Decisões Arquiteturais

### 2.1. Separação em Camadas (MVVM + Repository)
- **`MainActivity` (`ui`):** Atua estritamente como observadora do `LiveData<UiState>` e despachante de eventos de toque e contratos de permissão (`ActivityResultLauncher`). Não armazena dados de negócio nem executa chamadas de rede.
- **`DiarioViewModel` (`viewmodel`):** Retém a lista de registros da sessão, foto temporária e coordenadas GPS durante mudanças de configuração (como rotação de tela). Gerencia o `ExecutorService` (`Executors.newSingleThreadExecutor()`) e garante seu encerramento no método `onCleared()`.
- **`DiarioRepository` (`repository`):** Centraliza o armazenamento em memória da sessão e abstrai as chamadas síncronas do Retrofit executadas na thread em background, validando regras de contrato antes de devolver dados ao `ViewModel`.

### 2.2. Consumo Consciente de Energia e Hardware
- **Câmera:** Uso do contrato `ActivityResultContracts.TakePicturePreview()` para captura enxuta de miniatura `Bitmap` sob demanda, evitando alocações desnecessárias de alta resolução na memória RAM.
- **Localização:** Uso do `FusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)` para obter um **ponto único** sob demanda, evitando o alto consumo energético de *streaming* contínuo (`requestLocationUpdates`).
- **Sensores:** Registro do `SensorEventListener` em `onResume()` com `SensorManager.SENSOR_DELAY_UI` e cancelamento obrigatório em `onPause()` para impedir drenagem de bateria em background.

---

## 3. Gestão de Permissões

| Permissão | Categoria | Momento da Solicitação | Tratamento em Caso de Negação |
| :--- | :--- | :--- | :--- |
| `INTERNET` | Normal | Instalação (Manifest) | N/A (falhas de conectividade tratadas em `UiState.Error`) |
| `ACCESS_NETWORK_STATE` | Normal | Instalação (Manifest) | N/A |
| `CAMERA` | Perigosa (Runtime) | Apenas quando o usuário toca em **"1. Capturar Foto"** | Emite `UiState.PermissionDenied`, permite salvar o registro com ícone padrão e exibe botão para abrir as Configurações |
| `ACCESS_COARSE_LOCATION` e `ACCESS_FINE_LOCATION` | Perigosa (Runtime) | Apenas quando o usuário toca em **"2. Obter Posição GPS"** | Suporta localização aproximada (Android 12+) e, se negada, mantém o app funcional com coordenadas de fallback ou busca via CEP |

---

## 4. Contratos de Rede e Exemplos de Payloads JSON

### 4.1. ViaCEP API (`GET https://viacep.com.br/ws/{cep}/json/`)

#### Exemplo de Payload de Sucesso (`HTTP 200 OK`)
```json
{
  "cep": "38400-100",
  "logradouro": "Avenida Afonso Pena",
  "complemento": "até 1199/1200",
  "bairro": "Centro",
  "localidade": "Uberlândia",
  "uf": "MG",
  "ibge": "3170206",
  "gia": "",
  "ddd": "34",
  "siafi": "5403"
}
```

#### Exemplo de Payload de Erro Lógico (`HTTP 200 OK` com CEP Inexistente)
> **Nota Arquitetural:** O ViaCEP retorna código HTTP 200 mesmo quando o CEP não existe no banco postal. A classe `EnderecoViaCep` modela explicitamente o atributo `Boolean erro` para interceptar essa condição no `DiarioRepository` e lançar um `UiState.Error` instrutivo para o usuário.
```json
{
  "erro": true
}
```

---

### 4.2. OpenWeather API (`GET https://api.openweathermap.org/data/2.5/weather`)

Parâmetros enviados: `?lat=-18.9186&lon=-48.2772&appid={API_KEY}&units=metric&lang=pt_br`

#### Exemplo de Payload de Sucesso (`HTTP 200 OK`)
```json
{
  "coord": {
    "lon": -48.2772,
    "lat": -18.9186
  },
  "weather": [
    {
      "id": 801,
      "main": "Clouds",
      "description": "algumas nuvens",
      "icon": "02d"
    }
  ],
  "main": {
    "temp": 27.4,
    "feels_like": 28.1,
    "temp_min": 26.8,
    "temp_max": 28.5,
    "pressure": 1014,
    "humidity": 54
  },
  "name": "Uberlândia",
  "cod": 200
}
```

---

## 5. Como Executar e Testar os Cenários

1. Extraia o arquivo `.zip` e abra a pasta `DiarioDeCampoMovel` no **Android Studio**.
2. (Opcional) No arquivo `gradle.properties`, substitua `OPENWEATHER_API_KEY="SUA_CHAVE_AQUI"` pela sua chave da OpenWeather (nunca versionada em produção). Caso não informe uma chave, o `DiarioRepository` executa automaticamente o enriquecimento REST via **ViaCEP** usando o CEP digitado ou o CEP de referência das coordenadas.
3. Execute o app em um dispositivo físico ou emulador (API 26+):
   - **Cenário 1 (Três ou mais registros consecutivos):** Preencha o título, capture a foto, obtenha o GPS e salve 3 registros seguidos. Gire a tela do aparelho para validar a retenção de estado no `DiarioViewModel`.
   - **Cenário 2 (Enriquecimento via API REST):** Toque em um registro salvo para observar a transição `Loading` -> `Success` com os dados retornados da API.
   - **Cenário 3 (CEP Inválido / Erro Lógico):** Cadastre um registro com o CEP `00000000`, toque nele na lista e observe o tratamento do `{"erro": true}` no estado `Error`.
   - **Cenário 4 (Negação de Permissão):** Negue a permissão de câmera ou localização e verifique a mensagem explicativa e o botão de atalho para as Configurações do sistema.
   - **Cenário 5 (Ausência de Rede):** Ative o Modo Avião e toque em um registro para validar a captura de `IOException` sem travamento da interface.
