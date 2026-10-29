# Instruções de desenvolvimento — JJ Launcher

- Projeto Android nativo do zero, em Kotlin e Jetpack Compose.
- Prioridade: interface discreta, fluidez, acessibilidade e privacidade.
- Preserve o fluxo de launcher padrão (HOME) e o filtro de visibilidade restrito a atividades LAUNCHER.
- Não adicione internet, analytics, anúncios ou a permissão QUERY_ALL_PACKAGES sem requisito explícito e revisão.
- Mantenha a busca e as regras puras testáveis por testes unitários.
- Evite módulos, frameworks e abstrações prematuras.
- Antes de concluir uma alteração, execute `gradle :app:testDebugUnitTest :app:assembleDebug` ou relate por que não foi possível.
- Envie mudanças em branch e Pull Request; não faça merge sem autorização.
