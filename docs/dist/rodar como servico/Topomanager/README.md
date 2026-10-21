O **WinSW (Windows Service Wrapper)** é um utilitário de código aberto que permite empacotar qualquer aplicação executável ou script (como o Java) para rodar nativamente como um **Serviço do Windows**.

Ele resolve o problema de produção porque gerencia o ciclo de vida da aplicação: inicia com o Windows, reinicia automaticamente em caso de falha e redireciona os logs para arquivos.

Aqui está o passo a passo prático de como estruturar e usar o WinSW na pasta do seu projeto:

---

### Passo 1: Estrutura de Pastas no Servidor do Cliente

Na pasta onde está o seu sistema, organize os arquivos da seguinte forma:

```text
C:\TopoManager\
├── topomanager.jar
├── jdk-21.0.8\                <-- Sua pasta do Java 21
├── topomanager-service.exe    <-- O executável do WinSW renomeado
└── topomanager-service.xml    <-- O arquivo de configuração XML

```

---

### Passo 2: Obter o Executável do WinSW

1. Acesse o repositório oficial do WinSW no GitHub (ou baixe a versão estável mais recente para arquitetura de 64 bits).
2. O arquivo baixado geralmente se chama algo como `WinSW-x64.exe`.
3. Copie esse arquivo para a pasta do seu projeto e **renomeie-o** para um nome limpo, por exemplo: `topomanager-service.exe`.

---

### Passo 3: Criar o Arquivo de Configuração (`.xml`)

Na mesma pasta, crie um arquivo de texto chamado exatamente **`topomanager-service.xml`** com o conteúdo abaixo:

```xml
<service>
    <!-- ID único do serviço no Windows (sem espaços) -->
    <id>topomanager</id>
    
    <!-- Nome amigável que aparece na lista de Serviços do Windows -->
    <name>TopoManager Spring Boot</name>
    
    <!-- Descrição exibida no painel de serviços -->
    <description>Serviço backend do sistema TopoManager.</description>

    <!-- Caminho relativo para o Java.exe isolado na pasta local -->
    <executable>jdk-21.0.8\bin\java.exe</executable>

    <!-- Parâmetros da JVM e o jar da aplicação -->
    <arguments>-Xms2g -Xmx5g -XX:+UseG1GC -jar topomanager.jar</arguments>

    <!-- Configuração de diretório de trabalho -->
    <workingdirectory>.</workingdirectory>

    <!-- Comportamento caso a aplicação caia (reinicia após 10 segundos) -->
    <onfailure action="restart" delay="10sec"/>

    <!-- Configuração de Logs (substitui a tela preta do console) -->
    <log mode="roll-by-size">
        <sizeThreshold>10240</sizeThreshold>
        <keepFiles>8</keepFiles>
    </log>
</service>

```

---

### Passo 4: Instalar e Gerenciar o Serviço

Com os arquivos posicionados, abra o **Prompt de Comando como Administrador**, navegue até a pasta (`cd C:\TopoManager`) e execute os comandos de gerenciamento:

* **Para instalar o serviço no Windows:**
```cmd
topomanager-service.exe install

```


* **Para iniciar o serviço imediatamente:**
```cmd
topomanager-service.exe start

```


* **Para parar o serviço:**
```cmd
topomanager-service.exe stop

```


* **Para desinstalar o serviço (se precisar atualizar ou remover):**
```cmd
topomanager-service.exe uninstall

```



Pronto! Dando o `start`, o Java rodará em segundo plano consumindo os 5GB de RAM definidos, sem precisar de janelas abertas e iniciando junto com o servidor caso ele seja reiniciado.