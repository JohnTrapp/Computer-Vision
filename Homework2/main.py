import numpy as np
import cv2 as cv

cap = cv.VideoCapture(0)


def main():
    def onMouse( event, x, y, f, void ):
        pix = event.at<f>(y, x)
        b = pix.val[0]
        g = pix.val[1]
        r = pix.val[2]
        pass

    while (True):
        # Capture frame-by-frame
        status, frame = cap.read()

        # Our operations on the frame come here
        hsv = cv.cvtColor(frame, cv.COLOR_BGR2HSV)

        # define range of blue color in HSV
        lower_blue = np.array([110, 50, 50])
        upper_blue = np.array([130, 255, 255])

        # Threshold the HSV image to get only blue colors
        mask = cv.inRange(hsv, lower_blue, upper_blue)

        # Display the resulting frame
        cv.imshow('Lab 2', frame)
        cv.imshow('HSV View', hsv)
        cv.imshow('Object Tracking', mask)

        cv.setMouseCallback('HSV View', onMouse, 0)
        if cv.waitKey(1) & 0xFF == ord('q'):
            break


main()
# When everything done, release the capture
cap.release()
cv.destroyAllWindows()
