# JJ Launcher

Um launcher Android independente, minimalista e open source, desenvolvido do zero.

## Primeira versão (MVP)

- Tela inicial com relógio, data e papel de parede do celular.
- Dock local com até cinco aplicativos favoritos (segure um app para adicionar/remover).
- Gaveta com apps instalados, busca que ignora acentos e maiúsculas.
- Abertura de aplicativos, gesto de deslizar para cima e suporte ao botão Voltar.
- Solicitação da função de launcher padrão pelo botão **Padrão** na gaveta.
- Temas dinâmicos no Android 12+ na gaveta; contraste fixo na tela inicial.

## Abrir e executar

1. Abra o projeto no Android Studio com JDK 17.
2. Sincronize o Gradle e instale o Android SDK 36.
3. Execute o módulo `app` em um dispositivo ou emulador Android 8.0+.
4. Na gaveta, toque em **Padrão** para escolher o JJ Launcher como tela inicial.

Para compilar pela linha de comando com Gradle 8.13 instalado:

```sh
gradle :app:testDebugUnitTest :app:assembleDebug
```

O CI também gera um APK de debug para cada Pull Request, quando os testes passam.

## Tecnologias

Kotlin 2.3.21, Jetpack Compose (BOM 2026.09.00), Android Gradle Plugin 8.13.2; minSdk 26 e compileSdk 36.

## Privacidade

O aplicativo não solicita permissão de internet e não inclui SDKs de anúncios ou telemetria. Só consulta atividades lançáveis declaradas no manifesto. Favoritos ficam no armazenamento local da aplicação, sem backup automático.

## Limitações conhecidas do MVP

- Ainda não oferece widgets, pastas, múltiplas telas, arrastar ícones, pacotes de ícones ou atalhos avançados.
- Não implementa integração com a tela de recentes ou animações proprietárias de navegação do fabricante.
- É necessário verificar aparência, contraste, desempenho e comportamento de gestos em dispositivos reais.
- O ID de aplicação `dev.jj.launcher` e a marca JJ Launcher são provisórios.

## Desenvolvimento

Consulte [AGENTS.md](AGENTS.md). Contribuições são bem-vindas após validarmos o primeiro build.
