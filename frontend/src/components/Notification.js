const notificationHandler = (msg) => {
  // Notification body.
  const notification = document.createElement("div");

  notification.style.display = "flex";
  notification.style.width = "35ch";
  notification.style.height = "fit-content";
  notification.style.backgroundColor = "#b5824e";
  notification.style.color = "#fff";
  notification.style.borderRadius = "3px";
  notification.style.zIndex = "10000000";
  notification.style.position = "fixed";
  notification.style.alignItems = "center";
  notification.style.justifyContent = "center";
  notification.style.top = "14px";
  notification.style.right = "14px";
  notification.style.padding = "3px";

  // Notification text.
  const notificationText = document.createElement("p");
  notificationText.innerHTML = msg;
  notification.appendChild(notificationText);

  // Add to current page.
  document.body.appendChild(notification);

  setTimeout(() => {
    notification.style.display = "none";
  }, 3000);
};

export default notificationHandler;
