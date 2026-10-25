# Void Client

Dark neon utility client for **Minecraft Java Edition 1.21.11** (Fabric).

## Features (planned)

- Modular system (Combat, Movement, Render, Utility, HUD, Misc)
- ClickGUI com tema dark neon roxo
- Configurações tipadas + auto-save em JSON
- Event bus próprio
- HUD personalizável

## Build

### Requisitos
- JDK 21+
- Git

### Comandos

```bash
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

O jar final fica em `build/libs/void-client-1.0.0.jar`

### Desenvolvimento

```bash
./gradlew runClient
```

## Estrutura

```
src/main/java/dev/voidclient/
├── VoidClient.java          # Entrypoint
├── core/                    # Module, Event, Setting, Config (etapa b)
├── gui/                     # ClickGUI (etapa c)
├── modules/                 # Módulos
└── mixin/                   # Mixins
```

## Licença

MIT
