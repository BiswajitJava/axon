# Axon 🧠

**Your personal AI mentor for mastering the command line.**

Axon is an intelligent, AI-powered tutor that lives in your terminal. It's designed to accelerate your journey to mastering essential developer tools like Git, Docker, and Kubernetes. By generating interactive, step-by-step lessons on-demand, Axon helps you build the 'muscle memory' and deep conceptual understanding needed for true command-line proficiency.

---

<!-- It is highly recommended to create a short GIF of Axon in action and place it here.
     Tools like asciinema or terminalizer are perfect for this. -->
<!-- ![Axon Demo GIF](link-to-your-demo.gif) -->

## The Problem

Tired of dry documentation and forgettable tutorials? Learning complex command-line tools often involves passively reading walls of text, making it difficult to retain information and understand the practical "why" behind each command.

## The Solution

Axon transforms learning from a passive reading exercise into an **active, guided journey**. It uses the power of a state-of-the-art AI to generate a complete curriculum, tailored to your needs. You learn by doing, with an expert AI guide explaining the concepts behind every command, right in the environment where you'll use them: the terminal.

## ✨ Key Features

*   **AI-Generated Curriculum**: Get a fresh, comprehensive, and structured curriculum generated on-the-fly by a powerful language model.
*   **Interactive, Step-by-Step Lessons**: Progress through modules one lesson at a time with simple commands like `next`.
*   **Endless Learning with Pagination**: Finished a set of lessons? The `more` command uses the AI's "memory" of your progress to generate the next batch of advanced topics, ensuring you never run out of things to learn.
*   **Beautiful & Intuitive Interface**: A carefully designed, fully colorized terminal UI makes learning a pleasure, not a chore. The AI even provides color-tagged output for enhanced readability.
*   **Stateful Progress**: Axon saves your progress, so you can close the terminal and pick up right where you left off.
*   **Extensible by Design**: The architecture is built to easily support a growing library of developer tools.
*   **Self-Contained & Portable**: The entire application runs in a single, lightweight Docker container. All you need is Docker and an API key.

## 📚 Current & Future Modules

The beauty of Axon is its flexibility. The following modules are currently supported or planned:

*   [x] **Git**: Master version control from basics to advanced branching and history manipulation.
*   [ ] **Docker**: Learn how to build, manage, and run containers.
*   [ ] **Kubernetes (`kubectl`)**: Understand pods, deployments, and services.
*   [ ] **Linux Commands (`grep`, `awk`, `sed`)**: Become a command-line power user.
*   [ ] ...and more! Contributions are welcome.

## 🚀 Getting Started

Getting Axon running is simple, thanks to Docker.

### Prerequisites

1.  **Docker Desktop** installed and running on your system.
2.  An **API Key** from [Fireworks AI](https://fireworks.ai/). They have a generous free tier to get you started.

### Installation & Running

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/BiswajitJava/axon.git
    cd axon
    ```

2.  **Create your environment file:**
    Create a file named `.env` in the root of the project. This file will hold your secret API key and will be ignored by Git.
    ```
    # .env
    FIREWORKS_API_KEY=fw_...YourActualFireworksApiKeyGoesHere
    ```

3.  **Build the Docker image:**
    This command compiles the Java application and packages it into a portable Docker image named `axon`.
    ```powershell
    docker build -t axon .
    ```

4.  **Run the application:**
    This command starts the Axon CLI in your terminal. It securely loads your API key from the `.env` file into the container.
    ```powershell
    docker run -it --rm --env-file .env axon
    ```
    You should now see the `axon >` prompt, ready for your commands!

### A Note for Windows / PowerShell Users

The `--env-file` flag can sometimes be unreliable on certain Windows configurations due to file encoding or line ending issues. If you run `start <module>` and receive an authentication error, it means your API key was not loaded correctly.

In this case, you can use the more direct and robust `-e` flag to pass the key:

```powershell
# 1. Store your Fireworks key in a variable
$FW_API_KEY="fw_...YourActualFireworksApiKey"

# 2. Run the container, passing the key in directly
docker run -it --rm -e "FIREWORKS_API_KEY=$FW_API_KEY" axon
```
You should now see the `axon >` prompt, ready for your commands!

## 💻 Usage

Once inside the Axon shell, the commands are simple:

| Command           | Description                                                 |
| ----------------- | ----------------------------------------------------------- |
| `list`            | Shows all available learning modules.                       |
| `start <module>`  | Starts a new lesson plan for the specified module (e.g., `start basics`). |
| `next`            | Proceeds to the next lesson in the current module.          |
| `more`            | Generates the next batch of lessons when you finish a set.  |
| `status`          | Shows your current progress.                                |
| `exit`            | Exits the Axon CLI.                                         |

## 🛠️ Technology Stack

*   **Core:** Java 21 & Spring Boot 3
*   **CLI Framework:** Spring Shell
*   **AI Backend:** Fireworks AI (via OkHttp client)
*   **Containerization:** Docker

## 🙌 How to Contribute

Contributions are welcome! Whether it's adding a new module, improving a prompt, or fixing a bug, your help is appreciated. Please feel free to open an issue or submit a pull request.

1.  Fork the repository.
2.  Create your feature branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4.  Push to the branch (`git push origin feature/AmazingFeature`).
5.  Open a Pull Request.

## 📄 License

This project is licensed under the MIT License.
