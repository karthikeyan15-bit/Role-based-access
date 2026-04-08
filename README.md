 Role-Based Access Control System (RBAC)

A Java-based Role-Based Access Control (RBAC) system designed to manage authentication and authorization using structured role assignment. This project demonstrates how modern applications control access efficiently using roles instead of individual permissions.


Overview

Role-Based Access Control (RBAC) is a security model where users are assigned roles, and each role defines what actions can be performed.

This project implements a simple yet effective RBAC architecture using Java and web technologies.

RBAC is widely used in:

* Enterprise systems
* Admin dashboards
* Secure web applications

It enables scalable and maintainable access management. ([GitHub][1])



Features

* 🔑 User Authentication System
* 🛡️ Role-Based Authorization
* 👥 Multiple Roles (Admin, User, etc.)
* 📂 Clean Modular Structure
* 🌐 Basic Web Integration (HTML, CSS, JS)
* ⚡ Lightweight & Easy to Understand


🏗️ Project Structure

Java_project/
│
├── src/main/java/com/college/rbac/
│   ├── Main.java
│   ├── util/
│   │   └── JsonBuilder.java
│
├── src/main/resources/web/
│   ├── index.html
│   ├── css/
│   │   └── style.css
│   ├── js/
│   │   └── app.js
│
├── out/
├── run.bat


Technologies Used

* Java
* HTML
* CSS
* JavaScript
* JSON


How to Run

1. Clone the repository

```
git clone https://github.com/karthikeyan15-bit/Role-based-access.git
cd Role-based-access
```

2. Compile

```
javac src/main/java/com/college/rbac/Main.java
```

3. Run

```
java com.college.rbac.Main
```

OR (Windows)

```
run.bat
```

---

🧠 How It Works

* Users are assigned roles
* Roles define permissions
* Access is granted based on role
* No direct user-permission mapping

This keeps the system:

* Scalable
* Maintainable
* Secure


Use Cases

* Learning authentication & authorization
* Academic mini-projects
* Building secure backend systems
* Understanding real-world RBAC logic


Future Improvements

* Database Integration (MySQL / MongoDB)
* REST API Development
* JWT Authentication
* Admin Dashboard UI
* Role Hierarchy & Permission Management
