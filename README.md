# JJ Launcher

Um launcher Android independente, minimalista e open source, desenvolvido do zero.

## Primeira versão (MVP)

- Tela inicial com relógio, data e papel de parede do celular.
- Dock local com até cinco aplicativos favoritos (segure um app para adicionar/remover).
- Gaveta com apps instalados, busca que ignora acentos e maiúsculas.
- Abertura de aplicativos, gesto de deslizar para cima e suporte ao botão Voltar.
- Solicitação da função de launcher padrão pelo botão **Padrão** na gaveta.
- Atalho **Atualizações** na gaveta para consultar as versões oficiais no GitHub.
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

Kotlin 2.3.21, Jetpack Compose (BOM 2026.05.01), Android Gradle Plugin 8.13.2; minSdk 26 e compileSdk 36.

## Atualizações sem perder as configurações

Para instalar uma atualização por cima da versão anterior, o Android exige o **mesmo ID de aplicativo** (`dev.jj.launcher`), a **mesma chave de assinatura** e um `versionCode` compatível. Os favoritos locais são preservados ao atualizar; desinstalar o app apaga esses dados.

**Recomendado: [Obtainium](https://obtainium.imranr.dev/).** Instale o Obtainium e adicione `https://github.com/joaoldsxyzbr/Jj-launcher` como fonte GitHub, usando as versões estáveis (Releases). Ele acompanha as novas versões e orienta a instalação da atualização, com a confirmação do sistema Android quando necessária. O botão **Atualizações** dentro do launcher abre a página de versões; não há serviço de atualização em segundo plano no app.

**Importante:** APKs de debug gerados pelo CI podem usar assinaturas diferentes a cada execução e não são um canal de atualização. Instale o **APK release assinado** para começar o fluxo de atualizações. Não misture instalações de debug e release.

### Publicar uma versão assinada (mantenedor)

1. Gere e **guarde fora do repositório** um keystore privado (incluindo backup seguro); a chave não deve mudar entre versões. Exemplo:
   `keytool -genkeypair -v -keystore jj-launcher-release.jks -keyalg RSA -keysize 3072 -validity 10000 -alias jj-launcher`
2. Configure estes *GitHub Actions Secrets*: `JJ_KEYSTORE_BASE64` (keystore em base64, linha única), `JJ_STORE_PASSWORD`, `JJ_KEY_ALIAS` e `JJ_KEY_PASSWORD`. Não publique o keystore nem as senhas no Git.
3. Revise e incorpore à `main` o código que será publicado. Em **Actions → Publish signed release → Run workflow**, selecione `main` e indique uma versão como `0.1.0`.
4. O workflow testa o projeto, gera o APK release assinado, verifica a assinatura e publica a versão `v0.1.0` no GitHub Releases. Se faltar algum segredo, ele falha antes de publicar.
5. Nas próximas versões, aumente o número `MAJOR.MINOR.PATCH` (por exemplo, `0.1.1`) e repita o processo. A assinatura será a mesma e o Android reconhecerá a atualização.

`versionCode` para releases é derivado de `MAJOR * 1000000 + MINOR * 1000 + PATCH + 1`. Os componentes devem estar entre 0 e 999. Mantenha versões monotonamente crescentes.

O workflow de release só é iniciado manualmente a partir de `main`; o workflow de Pull Requests não recebe segredos de assinatura.

## Privacidade

O aplicativo não solicita permissão de internet e não inclui SDKs de anúncios ou telemetria. Só consulta atividades lançáveis declaradas no manifesto. Favoritos ficam no armazenamento local da aplicação, sem backup automático.

## Limitações conhecidas do MVP

- Ainda não oferece widgets, pastas, múltiplas telas, arrastar ícones, pacotes de ícones ou atalhos avançados.
- Não implementa integração com a tela de recentes ou animações proprietárias de navegação do fabricante.
- É necessário verificar aparência, contraste, desempenho e comportamento de gestos em dispositivos reais.
- O ID de aplicação `dev.jj.launcher` e a marca JJ Launcher são provisórios.

## Desenvolvimento

Consulte [AGENTS.md](AGENTS.md). Contribuições são bem-vindas após validarmos o primeiro build.
