# Neovim IDE Setup (VS Code / IntelliJ style)

A Neovim configuration based on **LazyVim**, with full Git tooling
(lazygit, gitsigns, diffview) for an IDE-like experience.

---

## 1. Requirements

- **Neovim >= 0.11.2** (check with `nvim --version`)
- **Git >= 2.19**
- **A C compiler** (for nvim-treesitter)
- **ripgrep** and **fd** (search)
- **lazygit** (Git UI)
- **Node.js** (needed by many language servers)
- **A Nerd Font** (e.g. JetBrainsMono Nerd Font), set as your terminal font

### macOS

```bash
brew install neovim git ripgrep fd lazygit node
brew install --cask font-jetbrains-mono-nerd-font
```

### Ubuntu / Debian

```bash
sudo apt install git ripgrep fd-find build-essential nodejs npm
```

> apt's Neovim is often too old. Install Neovim and lazygit from their
> GitHub releases instead.

---

## 2. Install LazyVim

```bash
# Back up any existing config
mv ~/.config/nvim ~/.config/nvim.bak
mv ~/.local/share/nvim ~/.local/share/nvim.bak

# Install the LazyVim starter
git clone https://github.com/LazyVim/starter ~/.config/nvim
rm -rf ~/.config/nvim/.git

# Launch (first start installs everything; this can take a few minutes)
nvim
```

---

## 3. Git tools (IntelliJ style)

Create the file `~/.config/nvim/lua/plugins/git.lua`:

```lua
return {
  -- Side-by-side diffs, file history, 3-way merge
  {
    "sindrets/diffview.nvim",
    cmd = { "DiffviewOpen", "DiffviewFileHistory" },
    keys = {
      { "<leader>gd", "<cmd>DiffviewOpen<cr>", desc = "Diff working tree" },
      { "<leader>gD", "<cmd>DiffviewClose<cr>", desc = "Close diff" },
      { "<leader>gh", "<cmd>DiffviewFileHistory %<cr>", desc = "File history" },
      { "<leader>gH", "<cmd>DiffviewFileHistory<cr>", desc = "Repo history" },
    },
    opts = { enhanced_diff_hl = true },
  },

  -- Inline blame on each line (like GitLens)
  {
    "lewis6991/gitsigns.nvim",
    opts = { current_line_blame = true },
  },
}
```

If any of these keys clash with a LazyVim default, change them.
Press `<leader>g` to see what's already taken.

Restart Neovim and the plugins install automatically.

### What each tool does

| Tool       | Use it for                                                  |
|------------|-------------------------------------------------------------|
| lazygit    | Commits, branches, rebase, stash, staging individual lines  |
| diffview   | Side-by-side diff, file history, 3-way merge conflicts      |
| gitsigns   | Change markers in the gutter, inline blame, hunk staging    |

**Merge conflicts:** run `:DiffviewOpen` during a merge to get a 3-way view.

---

## 4. Keyboard shortcuts

Leader key = `Space`

### General

| Action                      | Keys                  |
|-----------------------------|-----------------------|
| File explorer (sidebar)     | `Space e`             |
| Find file (like Ctrl+P)     | `Space Space`         |
| Search in project           | `Space /`             |
| Terminal                    | `Ctrl /`              |
| Show all shortcuts          | `Space` (then wait)   |

### Code

| Action                      | Keys                  |
|-----------------------------|-----------------------|
| Go to definition            | `gd`                  |
| Find references             | `gr`                  |
| Hover documentation         | `K`                   |
| Rename symbol               | `Space cr`            |
| Code actions (quick fix)    | `Space ca`            |
| Format file                 | `Space cf`            |

### Git

| Action                      | Keys                  |
|-----------------------------|-----------------------|
| Open lazygit                | `Space gg`            |
| Diff all changes            | `Space gd`            |
| Close diff                  | `Space gD`            |
| Current file history        | `Space gh`            |
| Repo history                | `Space gH`            |
| Next / previous hunk        | `]h` / `[h`           |
| Stage hunk                  | `Space ghs`           |
| Reset hunk                  | `Space ghr`           |

---

## 5. Language support

1. Run `:LazyExtras`.
2. Press `x` on the languages you use, for example:
   - `lang.typescript`
   - `lang.python`
   - `lang.java`
   - `lang.go`
3. Restart Neovim.

This sets up autocomplete, formatting and linting, like VS Code extensions do.

---

## 6. Maintenance

| Task                        | Command        |
|-----------------------------|----------------|
| Update plugins              | `:Lazy sync`   |
| Check setup health          | `:LazyHealth`  |
| Manage language servers     | `:Mason`       |
| Plugin manager UI           | `:Lazy`        |

---

## Config structure

```
~/.config/nvim/
├── init.lua
├── lazy-lock.json
├── lazyvim.json
└── lua/
    ├── config/
    │   ├── autocmds.lua
    │   ├── keymaps.lua
    │   ├── lazy.lua
    │   └── options.lua
    └── plugins/
        └── git.lua
```

Add your own plugins as new files in `lua/plugins/`. They load automatically.